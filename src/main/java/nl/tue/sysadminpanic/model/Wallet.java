package nl.tue.sysadminpanic.model;

import java.util.Locale;

/**
 * The player's money, in whole dollars.
 */
public class Wallet {

    private int balance;
    private int totalSpent;

    /**
     * Creates a wallet.
     *
     * @param startBalance the money the player starts with
     */
    public Wallet(int startBalance) {
        this.balance = startBalance;
    }

    /**
     * Formats an amount for display, for example {@code "$1,500"}.
     *
     * @param dollars the amount
     * @return the formatted amount
     */
    public static String format(int dollars) {
        // A fixed locale, so a Dutch system does not print "$1.500".
        return String.format(Locale.US, "$%,d", dollars);
    }

    /**
     * @param price the cost of something
     * @return true if the balance covers the price
     */
    public boolean canAfford(int price) {
        return balance >= price;
    }

    /**
     * Pays for something.
     *
     * @param price the amount to pay
     * @throws IllegalStateException if the balance does not cover the price
     */
    public void spend(int price) {
        if (!canAfford(price)) {
            throw new IllegalStateException("Cannot afford " + format(price));
        }
        balance -= price;
        totalSpent += price;
    }

    /**
     * Adds income.
     *
     * @param amount the amount to add
     */
    public void earn(int amount) {
        balance += amount;
    }

    /** @return the money currently available */
    public int getBalance() {
        return balance;
    }

    /** @return the total spent over the whole game */
    public int getTotalSpent() {
        return totalSpent;
    }
}
