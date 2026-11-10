package uz.melarium.payment;

import java.util.UUID;

import uz.melarium.domain.Investment;
import uz.melarium.domain.Payment;

public interface PaymentGateway {
    String provider();
    boolean isConfigured();
    CheckoutSession initiate(Payment payment, Investment order);

    record CheckoutSession(String redirectUrl, String providerReference) { }
}
