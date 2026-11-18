package fr.monlogcompta.service;

import fr.monlogcompta.service.InvoiceCalculator.Line;
import fr.monlogcompta.service.InvoiceCalculator.Totals;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceCalculatorTest {
    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    /** Jeu d'essai du cahier des charges, §11.2. */
    @Test
    void cahierDesChargesSampleInvoice() {
        Totals totals = InvoiceCalculator.compute(List.of(
            new Line(d("10"), d("50.00"), d("10"), d("20"), d("0")),
            new Line(d("1"), d("200.00"), d("0"), d("10"), d("0"))));
        assertEquals(d("650.00"), totals.totalHt());
        assertEquals(d("110.00"), totals.totalTva());
        assertEquals(d("760.00"), totals.totalTtc());
        assertEquals(2, totals.tvaParTaux().size());
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        // 3 x 0,335 = 1,005 -> 1,01 (HALF_UP) ; un double donnerait 1,00
        Line line = new Line(d("3"), d("0.335"), d("0"), d("20"), d("0"));
        assertEquals(d("1.01"), line.totalHt());
    }

    @Test
    void marginIsHtAfterDiscountMinusPurchaseCost() {
        Line line = new Line(d("12"), d("95"), d("0"), d("20"), d("42"));
        assertEquals(d("636.00"), line.marge());
    }
}
