package snail.app.flutter.google.pay;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterGooglePayPlugin
 */
public class FlutterGooglePayPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {
    private static final String CHANNEL_NAME = "flutter_google_pay";
    private final String METHOD_REQUEST_PAYMENT = "request_payment";
    private final String METHOD_REQUEST_CUSTOM_PAYMENT = "request_payment_custom_payment";
    private final String METHOD_IS_AVAILABLE = "is_available";
    private final String KEY_METHOD = "method_name";

    private Result mLastResult;
    private MethodCall mLastMethodCall;
    /**
     * Arbitrarily-picked constant integer you define to track a request for payment data activity.
     *
     * @value #LOAD_PAYMENT_DATA_REQUEST_CODE
     */
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;
    /**
     * A client for interacting with the Google Pay API.
     *
     * @see <a
     * href="https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient">PaymentsClient</a>
     */
    private PaymentsClient mPaymentsClient;
    private Activity mActivity;

    private FlutterGooglePayPlugin(Activity activity) {
        this.mActivity = activity;
    }

    private PaymentsClient client() {
        if (mPaymentsClient == null) {
            String environment = String.valueOf(mLastMethodCall.argument("environment"));
            int env = WalletConstants.ENVIRONMENT_TEST;
            if (environment.equals("production")) {
                env = WalletConstants.ENVIRONMENT_PRODUCTION;
            }
            mPaymentsClient =
                    Wallet.getPaymentsClient(mActivity,
                            new Wallet.WalletOptions.Builder().setEnvironment(env)
                                    .build());
        }
        return mPaymentsClient;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
        FlutterGooglePayPlugin plugin = new FlutterGooglePayPlugin(registrar.activity());
        registrar.addActivityResultListener(plugin);
        channel.setMethodCallHandler(plugin);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        mLastMethodCall = call;
        mLastResult = result;
        switch (call.method) {
            case METHOD_REQUEST_PAYMENT:
                this.requestPayment();
                break;
            case METHOD_IS_AVAILABLE:
                this.checkIsGooglePayAvailable();
                break;
            case (METHOD_REQUEST_CUSTOM_PAYMENT):
                this.requestPaymentCustom();
                break;
        }
    }

    /**
     * PaymentData response object contains the payment information, as well as any additional
     * requested information, such as billing and shipping address.
     *
     * @param paymentData A response object returned by Google after a payer approves payment.
     * @see <a
     * href="https://developers.google.com/pay/api/android/reference/object#PaymentData">Payment
     * Data</a>
     */
    private void callToDartOnPaymentSuccess(PaymentData paymentData) {
        JSONObject paymentMethodData = null;
        try {
            if (paymentData.getPaymentMethodToken() != null) {
                paymentMethodData = new JSONObject(paymentData.getPaymentMethodToken().getToken());
            }
            Log.d("PaymentData:", String.valueOf(paymentMethodData));
            Map<String, Object> data = new HashMap<>();
            data.put("status", paymentMethodData != null ? "SUCCESS" : "UNKNOWN");
            if (paymentMethodData != null) {
                data.put("result", paymentMethodData.toString());
            }
            mLastResult.success(data);
        } catch (JSONException e) {
            this.callToDartOnError(e.getMessage());
        }
    }

    private void callToDartOnGooglePayIsAvailable(boolean isAvailable) {
        if (mLastResult != null) {
            Map<String, Object> data = new HashMap<>();
            data.put(KEY_METHOD, METHOD_IS_AVAILABLE);
            data.put("isAvailable", isAvailable);
            mLastResult.success(data);
            mLastResult = null;
        }
    }

    private void callToDartOnError(String error) {
        if (mLastResult != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("error", error);
            mLastResult.success(data);
            mLastResult = null;
        }
    }

    private void callToDartOnCanceled() {
        if (mLastResult != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("status", "canceled");
            mLastResult.success(data);
            mLastResult = null;
        }
    }

    private void requestPaymentCustom() {
        Map map = mLastMethodCall.argument("custom_data");
        try {
            JSONObject paymentData = new JSONObject(map);
            PaymentDataRequest request =
                    PaymentDataRequest.fromJson(paymentData.toString());
            this.makePayment(request);
        } catch (Exception e) {
            callToDartOnError(e.getMessage());
        }
    }

    private void requestPayment() {
        String amount = mLastMethodCall.argument("amount");
        String currencyCode = mLastMethodCall.argument("currencyCode");
        String gateway = mLastMethodCall.argument("gateway");
        String stripeToken = mLastMethodCall.argument("stripeToken");
        String stripeVersion = mLastMethodCall.argument("stripeVersion");

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setTotalPrice(amount)
                .setCurrencyCode(currencyCode)
                .setGateway(gateway)
                .setStripeToken(stripeToken)
                .setStripeVersion(stripeVersion);

        PaymentDataRequest request =
                paymentInfo.createPaymentDataRequest(!TextUtils.isEmpty(stripeToken));
        this.makePayment(request);
    }

    private void makePayment(PaymentDataRequest request) {
        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            Task<PaymentData> task = client().loadPaymentData(request);
            AutoResolveHelper.resolveTask(task, mActivity, LOAD_PAYMENT_DATA_REQUEST_CODE);
        }
    }

    /**
     * Determine the viewer's ability to pay with a payment method supported by your app and display a
     * Google Pay payment button.
     *
     * @see <a href=
     * "https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient.html#isReadyToPay(com.google.android.gms.wallet.IsReadyToPayRequest)">PaymentsClient#IsReadyToPay</a>
     */
    private void checkIsGooglePayAvailable() {
        IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        Task<Boolean> task = client().isReadyToPay(request);
        task.addOnCompleteListener(mActivity,
                new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            callToDartOnGooglePayIsAvailable(true);

                        } else {
                            callToDartOnGooglePayIsAvailable(false);
                            Log.w("isReadyToPay failed", task.getException());
                        }
                    }
                });
    }

    /**
     * Handle a resolved activity from the Google Pay payment sheet.
     *
     * @param requestCode Request code originally supplied to AutoResolveHelper in requestPayment().
     * @param resultCode  Result code returned by the Google Pay API.
     * @param data        Intent from the Google Pay API containing payment or error data.
     * @see <a href="https://developer.android.com/training/basics/intents/result">Getting a result
     * from an Activity</a>
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    PaymentData paymentData = PaymentData.getFromIntent(data);
                    if (paymentData != null) {
                        this.callToDartOnPaymentSuccess(paymentData);
                    }
                    return true;
                case Activity.RESULT_CANCELED:
                    this.callToDartOnCanceled();
                    return true;
                case AutoResolveHelper.RESULT_ERROR:
                    Status status = AutoResolveHelper.getStatusFromIntent(data);
                    if (status != null) {
                        this.callToDartOnError(status.getStatusMessage());
                    }
                    return true;
            }
        }
        return false;
    }

}
