package snail.app.flutter.google.pay;

import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import java.util.Arrays;

final class PaymentInfo {
    private String mTotalPrice;
    private String mCurrencyCode;
    private String mGateway;
    private String mStripeToken;
    private String mStripeVersion;

    PaymentInfo() {
    }

    PaymentInfo setTotalPrice(String mTotalPrice) {
        this.mTotalPrice = mTotalPrice;
        return this;
    }

    PaymentInfo setCurrencyCode(String mCurrencyCode) {
        this.mCurrencyCode = mCurrencyCode;
        return this;
    }

    PaymentInfo setGateway(String mGateway) {
        this.mGateway = mGateway;
        return this;
    }

    PaymentInfo setStripeToken(String mStripeToken) {
        this.mStripeToken = mStripeToken;
        return this;
    }

    PaymentInfo setStripeVersion(String mStripeVersion) {
        this.mStripeVersion = mStripeVersion;
        return this;
    }


    private PaymentMethodTokenizationParameters createTokenizationParameters() {
        PaymentMethodTokenizationParameters.Builder builder = PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY);
        if (mGateway != null) {
            builder.addParameter("gateway", mGateway);
        }
        if (mStripeToken != null) {
            builder.addParameter("stripe:publishableKey", mStripeToken);
        }

        if (mStripeVersion != null) {
            builder.addParameter("stripe:version", mStripeVersion);
        }
        return builder.build();
    }

    PaymentDataRequest createPaymentDataRequest(boolean withTokenizationParameters) {
        PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                        .setTransactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                        .setTotalPrice(mTotalPrice)
                                        .setCurrencyCode(mCurrencyCode)
                                        .build())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .addAllowedCardNetworks(Arrays.asList(
                                                WalletConstants.CARD_NETWORK_AMEX,
                                                WalletConstants.CARD_NETWORK_DISCOVER,
                                                WalletConstants.CARD_NETWORK_VISA,
                                                WalletConstants.CARD_NETWORK_MASTERCARD))
                                        .build());
        if (withTokenizationParameters) {
            request.setPaymentMethodTokenizationParameters(createTokenizationParameters());
        }
        return request.build();
    }

}
