import 'package:flutter/material.dart';
import 'package:flutter_google_pay/flutter_google_pay.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  _makePayment() async {
    if (!(await FlutterGooglePay.isAvailable())) {
      _showToast(context, 'Google pay not available');
    } else {
      bool customData = false;

      if (!customData) {
        PaymentItem pm = PaymentItem(
            currencyCode: "usd",
            amount: "1",
            gateway: 'stripe',
            environment: 'test');

        FlutterGooglePay.makePayment(pm).then((dynamic result) {
          //TODO
        }).catchError((error) {
          //TODO
        });
      } else {
        //or
        ///docs https://developers.google.com/pay/api/android/guides/tutorial
        var jsonPayment = Map();

        FlutterGooglePay.makeCustomPayment(jsonPayment).then((dynamic result) {
          //TODO
        }).catchError((error) {
          //TODO
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: FlatButton(
            onPressed: _makePayment,
            child: Text('Pay'),
          ),
        ),
      ),
    );
  }

  void _showToast(BuildContext context, String message) {
    final scaffold = Scaffold.of(context);
    scaffold.showSnackBar(SnackBar(
      content: Text(message),
      action: SnackBarAction(
        label: 'UNDO',
        onPressed: () {},
      ),
    ));
  }
}
