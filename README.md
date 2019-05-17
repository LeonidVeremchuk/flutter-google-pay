# flutter_google_pay
[![pub](https://img.shields.io/pub/v/flutter_google_pay.svg)](https://pub.dev/packages/flutter_google_pay)

Accept Payments with Android Pay using the [Payment Request API](https://developers.google.com/pay/api/android/overview).

## Usage

```dart
  import 'package:flutter_google_pay/flutter_google_pay.dart';
  
  
  _makePayment() async {
    if (!(await FlutterGooglePay.isAvailable())) {
       //_showToast(context, 'Google pay not available');
    } else {
      bool customData = false;
      if (!customData) {
        PaymentItem pm = PaymentItem(
            currencyCode: "usd",
            amount: "1.0",
            gateway: 'stripe',
            environment: 'test');
        FlutterGooglePay.makePayment(pm).then((Result result) {
          if (result.status == ResultStatus.SUCCESS) {
            //Success!
          }
        }).catchError((error) {
          //unresolved error
        });
      } 
    }
  }
  
  //or
   _makeCustomPayment() async {
     if (!(await FlutterGooglePay.isAvailable())) {
       _showToast(context, 'Google pay not available');
     } else {
         var jsonPayment = Map();
         FlutterGooglePay.makeCustomPayment(jsonPayment).then((dynamic result) {
           if (result.status == ResultStatus.SUCCESS) {
             //Success!
           }
         }).catchError((error) {
           //unresolved error
         });
     }
  }
  
```
### Doc for creating custom payment data:

 *[Google Pay](https://developers.google.com/pay/api/android/guides/tutorial)



