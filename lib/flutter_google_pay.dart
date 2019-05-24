import "dart:async";
import 'dart:convert';
import "dart:io";

import 'package:flutter/foundation.dart';
import "package:flutter/services.dart";

class FlutterGooglePay {
  static const MethodChannel _channel =
      const MethodChannel("flutter_google_pay");

  static Future<Result> makePayment(PaymentItem data) async {
    return _call("request_payment", data.toMap());
  }

  static Future<Result> makeCustomPayment(Map data) async {
    return _call("request_payment_custom_payment", data);
  }

  static Future<Result> _call(String methodName, dynamic data) async {
    Result result =
        await _channel.invokeMethod(methodName, data).then((dynamic data) {
      return _parseResult(data);
    }).catchError((dynamic error) {
      return Result(error?.toString() ?? 'unknow error', null,
          ResultStatus.ERROR, (error?.toString()) ?? "");
    });
    if (result != null) {
      return result;
    }
    return Result('unknow', null, ResultStatus.UNKNOWN, "");
  }

  static Future<bool> isAvailable(String environment) async {
    if (!Platform.isAndroid) {
      return false;
    }
    try {
      Map map = await _channel
          .invokeMethod("is_available", {"environment": environment});
      return map['isAvailable'];
    } catch (error) {
      return false;
    }
  }

  static Result _parseResult(dynamic map) {
    var error = map['error'];
    var status = map['status'];
    var result = map['result'];
    var description = map["description"];
    if (result != null) {
      result = json.decode(result);
    }
    ResultStatus resultStatus;
    if (error != null) {
      resultStatus = ResultStatus.ERROR;
    } else if (status != null) {
      resultStatus = parseStatus(status);
    } else if (result != null) {
      resultStatus = ResultStatus.SUCCESS;
    } else {
      resultStatus = ResultStatus.UNKNOWN;
    }
    return Result(error, result, resultStatus, description);
  }

  static ResultStatus parseStatus(String status) {
    switch (status) {
      case "SUCCESS":
        return ResultStatus.SUCCESS;
      case "ERROR":
        return ResultStatus.ERROR;
      case "RESULT_CANCELED":
        return ResultStatus.RESULT_CANCELED;
      case "RESULT_INTERNAL_ERROR":
        return ResultStatus.RESULT_INTERNAL_ERROR;
      case "DEVELOPER_ERROR":
        return ResultStatus.DEVELOPER_ERROR;
      case "RESULT_TIMEOUT":
        return ResultStatus.RESULT_TIMEOUT;
      case "RESULT_DEAD_CLIENT":
        return ResultStatus.RESULT_DEAD_CLIENT;
      default:
        return ResultStatus.UNKNOWN;
    }
  }
}

class PaymentItem {
  String currencyCode;
  String amount;
  String gateway;
  String stripeToken;
  String stripeVersion;

  PaymentItem(
      {@required this.currencyCode,
      @required this.amount,
      @required this.gateway,
      @required this.stripeToken,
      @required this.stripeVersion});

  Map toMap() {
    Map args = Map();
    args["amount"] = amount;
    args["currencyCode"] = currencyCode;
    if (!_validateAmount(amount)) {
      throw Exception("Wrong amount: ${amount ?? "unknow"}");
    }
    if (!_validateCurrencyCode(currencyCode)) {
      throw Exception("Wrong currency code: ${currencyCode ?? "unknow"}");
    }

    args["gateway"] = gateway;
    args["stripeToken"] = stripeToken;
    args["stripeVersion"] = stripeVersion;

    return args;
  }
}

enum ResultStatus {
  SUCCESS,
  ERROR,
  RESULT_CANCELED,
  RESULT_INTERNAL_ERROR,
  DEVELOPER_ERROR,
  RESULT_TIMEOUT,
  RESULT_DEAD_CLIENT,
  UNKNOWN,
}

class Result {
  String error;
  String description;
  Map data;
  ResultStatus status;

  Result(this.error, this.data, this.status, this.description);
}

bool _validateAmount(dynamic amount) {
  return (amount?.toString() ?? "").length > 0 ?? false;
}

bool _validateCurrencyCode(dynamic currencyCode) {
  bool isNotEmpty = (currencyCode?.toString() ?? "").length > 0 ?? false;
  if (!isNotEmpty) {
    return false;
  }

//  String lowerCaseCode = currencyCode.toString().toLowerCase();
  //TODO currency check
  return true;
}

class PaymentBuilder {
  Map<String, dynamic> _gatewayTokenizationSpecification;
  Map<String, dynamic> _directTokenizationSpecification;
  Map<String, dynamic> _transactionInfo;
  Map<String, dynamic> _merchantInfo;
  List<String> _allowedCardNetworks;
  List<String> _allowedCardAuthMethods;
  List<String> _shippingSupportedCountries;
  bool _billingAddressRequired;
  bool _shippingAddressRequired;
  bool _phoneNumberRequred;

  /// An object describing information requested in a Google Pay payment sheet
  ///
  /// @return Payment data expected by your app.
  Map build() {
    Map paymentDataRequest = _baseRequest;
    paymentDataRequest["allowedPaymentMethods"] = [_cardPaymentMethod];
    if (_transactionInfo == null) {
      throw Exception('Please provide transaction info');
    }
    paymentDataRequest["transactionInfo"] = _transactionInfo;
    if (_merchantInfo != null) {
      paymentDataRequest["merchantInfo"] = _merchantInfo;
    }
    if (_shippingAddressRequired != null) {
      paymentDataRequest["shippingAddressRequired"] = _shippingAddressRequired;
    }
    Map shippingAddressParameters = Map();
    if (_phoneNumberRequred != null) {
      shippingAddressParameters["phoneNumberRequired"] = _phoneNumberRequred;
    }
    if (_shippingSupportedCountries != null) {
      List allowedCountryCodes = _shippingSupportedCountries;
      shippingAddressParameters["allowedCountryCodes"] = allowedCountryCodes;
      paymentDataRequest["shippingAddressParameters"] =
          shippingAddressParameters;
    }
    return paymentDataRequest;
  }

  /// Gateway Integration: Identify your gateway and your app's gateway merchant identifier.
  ///   *
  ///   * <p>The Google Pay API response will return an encrypted payment method capable of being charged
  ///   * by a supported gateway after payer authorization.
  ///   *
  addGateway([String gateway, String gatewayMerchantId]) {
    if (_directTokenizationSpecification != null) {
      throw Exception(
          "You already set a DIRRECT. You can use DIRECT or Gateway.");
    }
    Map gateway = Map();
    gateway["gateway"] = gateway;
    if (!isEmpty(gatewayMerchantId)) {
      gateway["gatewayMerchantId"] = gatewayMerchantId;
    }
    _gatewayTokenizationSpecification = {
      "type": "PAYMENT_GATEWAY",
      "parameters": gateway
    };
  }

  /// {@code DIRECT} Integration: Decrypt a response directly on your servers. This configuration has
  /// additional data security requirements from Google and additional PCI DSS compliance complexity.
  ///
  /// <p>Please refer to the documentation for more information about {@code DIRECT} integration. The
  /// type of integration you use depends on your payment processor.
  addDirectTokenizationSpecification(String directTokenizationPublikKey,
      {String protocolVersion = "ECv2"}) {
    if (_gatewayTokenizationSpecification != null) {
      throw Exception(
          "You already set a gateway. You can use DIRECT or Gateway.");
    }
    if (isEmpty(directTokenizationPublikKey)) {
      throw Exception("Please add protocol version & public key.");
    }
    Map directTokenizationParameters = {
      "protocolVersion": protocolVersion,
      "publicKey": directTokenizationPublikKey
    };
    _directTokenizationSpecification = {
      "type": "DIRECT",
      "parameters": directTokenizationParameters
    };
  }

  /// Provide Google Pay API with a payment amount, currency, and amount status.
  addTransactionInfo(String price, String currencyCode) {
    _transactionInfo = {
      "totalPrice": price,
      "totalPriceStatus": "FINAL",
      "currencyCode": currencyCode,
    };
  }

  /// Information about the merchant requesting payment information
  addMerchantInfo(String info) {
    _merchantInfo = {"merchantName": info};
  }

  /// Card networks supported by your app and your gateway.
  /// Card networks:
  ///    "AMEX",
  ///    "DISCOVER",
  ///    "JCB",
  ///    "MASTERCARD",
  ///    "VISA"
  addAllowedCardNetworks(List<String> allowedCardNetworks) {
    if (allowedCardNetworks != null && allowedCardNetworks.length > 0) {
      _allowedCardNetworks = allowedCardNetworks;
    }
  }

  /// Card authentication methods supported by your app and your gateway.
  /// Card methods:
  ///    "PAN_ONLY"
  ///    "CRYPTOGRAM_3DS"
  ///
  /// The Google Pay API may return cards on file on Google.com (PAN_ONLY) and/or a device token on
  /// an Android device authenticated with a 3-D Secure cryptogram (CRYPTOGRAM_3DS).
  addAllowedCardAuthMethods(List<String> allowedCardAuthMethods) {
    if (allowedCardAuthMethods != null && allowedCardAuthMethods.length > 0) {
      _allowedCardAuthMethods = allowedCardAuthMethods;
    }
  }

  /// Optionally, you can add billing address/phone number associated with a CARD payment method.
  ///
  /// Please, skipp this function call if no need to add this parameter.
  addBillingAddressRequired(bool required) {
    _billingAddressRequired = required;
  }

  /// An optional shipping address requirement is a top-level property
  ///
  /// Please, skipp this function call if no need to add this parameter.
  addShippingAddressRequired(bool required) {
    _shippingAddressRequired = required;
  }

  addPhoneNumberRequired(bool required) {
    _phoneNumberRequred = required;
  }

  /// Supported countries for shipping (use ISO 3166-1 alpha-2 country codes). Relevant only when
  /// requesting a shipping address.
  addShippingSupportedCountries(List<String> shippingSupportedCountries) {
    if (shippingSupportedCountries != null &&
        shippingSupportedCountries.length > 0) {
      _shippingSupportedCountries = shippingSupportedCountries;
    }
  }

  /// Create a Google Pay API base request object with properties used in all requests.
  /// @return Google Pay API base request object.
  Map get _baseRequest {
    return {"apiVersion": 2, "apiVersionMinor": 0};
  }

  /// Describe your app's support for the CARD payment method.
  /// The provided properties are applicable to both an IsReadyToPayRequest and a
  /// PaymentDataRequest.
  ///
  /// @return A CARD PaymentMethod object describing accepted cards.
  Map get _baseCardPaymentMethod {
    Map cardPaymentMethod = Map();
    cardPaymentMethod["type"] = "CARD";
    Map parameters = new Map();
    if (_allowedCardNetworks == null) {
      throw Exception("Please provide information about card networds");
    }
    if (_allowedCardAuthMethods == null) {
      throw Exception("Please provide information about card auth methods");
    }
    parameters["allowedAuthMethods"] = _allowedCardAuthMethods;
    parameters["allowedCardNetworks"] = _allowedCardNetworks;
    if (_billingAddressRequired != null) {
      parameters["billingAddressRequired"] = _billingAddressRequired;
    }
    Map billingAddressParameters = Map();
    billingAddressParameters["format"] = "FULL";
    parameters["billingAddressParameters"] = billingAddressParameters;
    cardPaymentMethod["parameters"] = parameters;
    return cardPaymentMethod;
  }

  /// Describe the expected returned payment data for the CARD payment method
  ///
  /// @return A CARD PaymentMethod describing accepted cards and optional fields.
  Map get _cardPaymentMethod {
    Map cardPaymentMethod = _baseCardPaymentMethod;
    if (_gatewayTokenizationSpecification != null ||
        _directTokenizationSpecification != null) {
      cardPaymentMethod["tokenizationSpecification"] =
          _gatewayTokenizationSpecification ?? _directTokenizationSpecification;
    }
    return cardPaymentMethod;
  }
}

bool isEmpty(String value) {
  return value == null || value.length == 0;
}
