### example/flutter_google_pay_example

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