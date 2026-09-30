package nl.tue.sysadminpanic.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Wallet}.
 */
class WalletTest {

    @Test
    void spendingLowersBalanceAndCountsTotalSpent() {
        Wallet wallet = new Wallet(1500);
        wallet.spend(300);
        wallet.spend(200);
        assertEquals(1000, wallet.getBalance());
        assertEquals(500, wallet.getTotalSpent());
    }

    @Test
    void earningRaisesBalanceButNotTotalSpent() {
        Wallet wallet = new Wallet(100);
        wallet.earn(50);
        assertEquals(150, wallet.getBalance());
        assertEquals(0, wallet.getTotalSpent());
    }

    @Test
    void exactBalanceIsAffordable() {
        Wallet wallet = new Wallet(800);
        assertTrue(wallet.canAfford(800));
        assertFalse(wallet.canAfford(801));
    }

    @Test
    void spendingMoreThanBalanceThrowsAndChangesNothing() {
        Wallet wallet = new Wallet(100);
        assertThrows(IllegalStateException.class, () -> wallet.spend(200));
        assertEquals(100, wallet.getBalance());
    }

    @Test
    void formatUsesDollarSignAndThousandsComma() {
        assertEquals("$1,500", Wallet.format(1500));
        assertEquals("$300", Wallet.format(300));
        assertEquals("$0", Wallet.format(0));
    }
}
