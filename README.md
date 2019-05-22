# flutter_google_pay
[![pub](https://img.shields.io/pub/v/flutter_google_pay.svg)](https://pub.dev/packages/flutter_google_pay)

Accept Payments with Android Pay using the [Payment Request API](https://developers.google.com/pay/api/android/overview).

## Usage

```dart
  import 'package:flutter_google_pay/flutter_google_pay.dart';
  
  
  _makeStripePayment() async {
      var environment = 'rest'; // or 'production'

      if (!(await FlutterGooglePay.isAvailable(environment))) {
        _showToast(scaffoldContext, 'Google pay not available');
      } else {
        PaymentItem pm = PaymentItem(
            stripeToken: 'pk_test_1IV5H8NyhgGYOeK6vYV3Qw8f',
            stripeVersion: "2018-11-08",
            currencyCode: "usd",
            amount: "0.10",
            gateway: 'stripe');

        FlutterGooglePay.makePayment(pm).then((Result result) {
          if (result.status == ResultStatus.SUCCESS) {
            _showToast(scaffoldContext, 'Success');
          }
        }).catchError((dynamic error) {
          _showToast(scaffoldContext, error.toString());
        });
      }
    }

    _makeCustomPayment() async {
      var environment = 'rest'; // or 'production'

      if (!(await FlutterGooglePay.isAvailable(environment))) {
        _showToast(scaffoldContext, 'Google pay not available');
      } else {
        ///docs https://developers.google.com/pay/api/android/guides/tutorial
        PaymentBuilder pb = PaymentBuilder()
          ..addGateway("example")
          ..addTransactionInfo("1.0", "USD")
          ..addAllowedCardAuthMethods(["PAN_ONLY", "CRYPTOGRAM_3DS"])
          ..addAllowedCardNetworks(
              ["AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA"])
          ..addBillingAddressRequired(true)
          ..addPhoneNumberRequired(true)
          ..addShippingAddressRequired(true)
          ..addShippingSupportedCountries(["US", "GB"])
          ..addMerchantInfo("Example");

        FlutterGooglePay.makeCustomPayment(pb.build()).then((Result result) {
          if (result.status == ResultStatus.SUCCESS) {
            _showToast(scaffoldContext, 'Success');
          } else if (result.error != null) {
            _showToast(context, result.error);
          }
        }).catchError((error) {
          //TODO
        });
      }
    }
  
```
### Doc for creating custom payment data:

 [Google Pay](https://developers.google.com/pay/api/android/guides/tutorial)



