package snail.app.flutter.google.pay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;

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

    private enum Error {
        WRONG_AMOUNT,
    }


    private FlutterGooglePayPlugin(Activity activity) {
        this.mActivity = activity;
        mPaymentsClient = PaymentsUtil.createPaymentsClient(activity);
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
        FlutterGooglePayPlugin plugin = new FlutterGooglePayPlugin(registrar.activity());
        channel.setMethodCallHandler(plugin);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        mLastMethodCall = call;
        mLastResult = result;
        if (call.method.equals(METHOD_REQUEST_PAYMENT)) {
            this.requestPayment();
        } else if (call.method.equals(METHOD_IS_AVAILABLE)) {
            this.checkIsGooglePayAvailable();
        }
    }

    private void callToDartOnGooglePayAvailable() {
        if (mLastResult == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put(KEY_METHOD, METHOD_IS_AVAILABLE);
        data.put("isAvailable", true);
        mLastResult.success(data);
    }

    private void requestPayment() {
        if (mLastMethodCall == null) {
            return;
        }
        validatePayment();
        String amount = String.valueOf(mLastMethodCall.argument("amount"));
        String currencyCode = String.valueOf(mLastMethodCall.argument("currencyCode"));


        // TransactionInfo transaction = PaymentsUtil.createTransaction(price);
        JSONObject paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(amount, currencyCode);
        if (paymentDataRequestJson == null) {
            return;
        }
        PaymentDataRequest request =
                PaymentDataRequest.fromJson(paymentDataRequestJson.toString());

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            AutoResolveHelper.resolveTask(
                    mPaymentsClient.loadPaymentData(request), mActivity, LOAD_PAYMENT_DATA_REQUEST_CODE);
        }
    }

    private void validatePayment() {
        Object amount = mLastMethodCall.argument("amount");
        if (!(amount instanceof Integer) && !(amount instanceof String)) {
            mLastResult.error(Error.WRONG_AMOUNT.name(), "Wrong amount", amount);
        }

        Object currencyCode = mLastMethodCall.argument("currencyCode");
        if (!(currencyCode instanceof Integer) && !(currencyCode instanceof String)) {
            mLastResult.error(Error.WRONG_AMOUNT.name(), "Wrong amount", currencyCode);
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
        final JSONObject isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest();
        if (isReadyToPayJson == null) {
            return;
        }
        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString());
        if (request == null) {
            return;
        }

        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(mActivity,
                new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            callToDartOnGooglePayAvailable();

                        } else {
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
                        handlePaymentSuccess(paymentData);
                    }
                    return true;
                case Activity.RESULT_CANCELED:
                    return true;
                case AutoResolveHelper.RESULT_ERROR:
                    Status status = AutoResolveHelper.getStatusFromIntent(data);
                    if (status != null) {
                        handleError(status.getStatusCode());
                    }
                    return true;
            }
        }
        return false;
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
    private void handlePaymentSuccess(PaymentData paymentData) {
        String paymentInformation = paymentData.toJson();

        // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
        if (paymentInformation == null) {
            return;
        }
        JSONObject paymentMethodData;

        try {
            paymentMethodData = new JSONObject(paymentInformation).getJSONObject("paymentMethodData");
            // If the gateway is set to "example", no payment information is returned - instead, the
            // token will only consist of "examplePaymentMethodToken".
            if (paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("type")
                    .equals("PAYMENT_GATEWAY")
                    && paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
                    .equals("examplePaymentMethodToken")) {
                AlertDialog alertDialog =
                        new AlertDialog.Builder(mActivity)
                                .setTitle("Warning")
                                .setMessage(
                                        "Gateway name set to \"example\" - please modify "
                                                + "Constants.java and replace it with your own gateway.")
                                .setPositiveButton("OK", null)
                                .create();
                alertDialog.show();
            }

            String billingName =
                    paymentMethodData.getJSONObject("info").getJSONObject("billingAddress").getString("name");
            Log.d("BillingName", billingName);
            Toast.makeText(mActivity, "Success" + billingName, Toast.LENGTH_LONG)
                    .show();

            // Logging token string.
            Log.d("GooglePaymentToken", paymentMethodData.getJSONObject("tokenizationData").getString("token"));
        } catch (JSONException e) {
            Log.e("handlePaymentSuccess", "Error: " + e.toString());
        }
    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
     *                   WalletConstants.ERROR_CODE_* constants.
     * @see <a
     * href="https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants#constant-summary">
     * Wallet Constants Library</a>
     */
    private void handleError(int statusCode) {
        Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode));
    }
}
