# flutter_google_pay
[![pub](https://img.shields.io/pub/v/flutter_google_pay.svg)](https://pub.dev/packages/flutter_google_pay)

Accept Payments with Android Pay using the [Payment Request API](https://developers.google.com/pay/api/android/overview).

## Usage

```dart
  import 'package:flutter_google_pay/flutter_google_pay.dart';
  
  
  _makePayment() async {
    var environment = "test";  /// or production 
    
    if (!(await FlutterGooglePay.isAvailable(environment))) {
       //_showToast(context, 'Google pay not available');
    } else {
      bool customData = false;
      if (!customData) {
        PaymentItem pm = PaymentItem(
               stripeToken: 'pk_test_1IV5H8NyhgGYOeK6vYV3Qw8f',
               stripeVersion: "2018-11-08",
               currencyCode: "usd",
               amount: "0.10",
               gateway: 'stripe');
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
     var environment = "test";  /// or production 
     
     if (!(await FlutterGooglePay.isAvailable(environment))) {
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

 [Google Pay](https://developers.google.com/pay/api/android/guides/tutorial)



