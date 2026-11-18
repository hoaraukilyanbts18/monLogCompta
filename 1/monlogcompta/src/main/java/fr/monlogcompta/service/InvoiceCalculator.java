package fr.monlogcompta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calculs de facture (EF-16 à EF-20, règles 5.1) : tout en BigDecimal, arrondi HALF_UP à 2 décimales.
 */
public final class InvoiceCalculator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private InvoiceCalculator() {
    }

    /** Ligne de facture ; remisePourcent en % (0 si aucune), coutAchat = coût d'achat HT unitaire. */
    public record Line(BigDecimal quantite, BigDecimal prixUnitaireHt, BigDecimal remisePourcent,
                       BigDecimal tauxTva, BigDecimal coutAchat) {
        public BigDecimal totalHt() {
            BigDecimal brut = quantite.multiply(prixUnitaireHt);
            BigDecimal remise = brut.multiply(remisePourcent).divide(HUNDRED, 10, RoundingMode.HALF_UP);
            return round(brut.subtract(remise));
        }

        public BigDecimal marge() {
            return round(totalHt().subtract(quantite.multiply(coutAchat)));
        }
    }

    public record Totals(BigDecimal totalHt, Map<BigDecimal, BigDecimal> tvaParTaux,
                         BigDecimal totalTva, BigDecimal totalTtc, BigDecimal marge) {
    }

    public static Totals compute(List<Line> lines) {
        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal marge = BigDecimal.ZERO;
        Map<BigDecimal, BigDecimal> baseParTaux = new TreeMap<>();
        for (Line line : lines) {
            BigDecimal ht = line.totalHt();
            totalHt = totalHt.add(ht);
            marge = marge.add(line.marge());
            baseParTaux.merge(line.tauxTva().stripTrailingZeros(), ht, BigDecimal::add);
        }
        // La TVA est calculée par taux sur la base HT de chaque taux, puis arrondie au centime.
        Map<BigDecimal, BigDecimal> tvaParTaux = new TreeMap<>();
        BigDecimal totalTva = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, BigDecimal> entry : baseParTaux.entrySet()) {
            BigDecimal tva = round(entry.getValue().multiply(entry.getKey()).divide(HUNDRED, 10, RoundingMode.HALF_UP));
            tvaParTaux.put(entry.getKey(), tva);
            totalTva = totalTva.add(tva);
        }
        return new Totals(round(totalHt), tvaParTaux, round(totalTva), round(totalHt.add(totalTva)), round(marge));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
