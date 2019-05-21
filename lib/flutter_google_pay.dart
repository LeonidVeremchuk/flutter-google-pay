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
      return Result(
          error?.toString() ?? 'unknow error', null, ResultStatus.ERROR);
    });
    if (result != null) {
      return result;
    }
    return Result('unknow', null, ResultStatus.UNKNOWN);
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
    if (result != null) {
      result = json.decode(result);
    }

    ResultStatus resultStatus;

    if (error != null) {
      resultStatus = ResultStatus.ERROR;
    } else if (status == 'canceled') {
      resultStatus = ResultStatus.CANCELED;
    } else if (result != null) {
      resultStatus = ResultStatus.SUCCESS;
    } else {
      resultStatus = ResultStatus.UNKNOWN;
    }
    return Result(error, result, resultStatus);
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
    args['amount'] = amount;
    args['currencyCode'] = currencyCode;
    if (!_validateAmount(amount)) {
      throw Exception("Wrong amount: ${amount ?? 'unknow'}");
    }
    if (!_validateCurrencyCode(currencyCode)) {
      throw Exception("Wrong currency code: ${currencyCode ?? 'unknow'}");
    }

    args['gateway'] = gateway;
    args['stripeToken'] = stripeToken;
    args['stripeVersion'] = stripeVersion;

    return args;
  }
}

enum ResultStatus {
  SUCCESS,
  ERROR,
  CANCELED,
  UNKNOWN,
}

class Result {
  String error;
  Map data;
  ResultStatus status;

  Result(this.error, this.data, this.status);
}

bool _validateAmount(dynamic amount) {
  return (amount?.toString() ?? '').length > 0 ?? false;
}

bool _validateCurrencyCode(dynamic currencyCode) {
  bool isNotEmpty = (currencyCode?.toString() ?? '').length > 0 ?? false;
  if (!isNotEmpty) {
    return false;
  }

//  String lowerCaseCode = currencyCode.toString().toLowerCase();
  //TODO currency check
  return true;
}
