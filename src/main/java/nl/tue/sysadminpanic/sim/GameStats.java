package nl.tue.sysadminpanic.sim;

/**
 * The numbers shown in the end-of-game summary.
 *
 * @param won                true for "NETWORK SECURED", false for "NETWORK BREACHED"
 * @param finalHealth        network health at the end, from 0 to 100
 * @param wavesSurvived      how many waves were completed
 * @param totalWaves         how many waves the scenario has
 * @param threatsDetected    campaigns the SOC had evidence of (a real alert or an inspection)
 * @param threatsContained   campaigns stopped or contained before they finished
 * @param totalThreats       all campaigns that were started
 * @param systemsCompromised distinct devices that were compromised at least once
 * @param moneySpent         total dollars spent on actions
 */
public record GameStats(
        boolean won,
        double finalHealth,
        int wavesSurvived,
        int totalWaves,
        int threatsDetected,
        int threatsContained,
        int totalThreats,
        int systemsCompromised,
        int moneySpent) {
}
