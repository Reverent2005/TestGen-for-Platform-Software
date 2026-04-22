/*
 * Spec file for BankAccount
 * Contract definitions for account operations
 */

spec deposit {
    signature: void deposit(double amount);
    requires:  amount > 0.0;
    ensures:   balance == \old(balance) + amount;
}

spec withdraw {
    signature: boolean withdraw(double amount);
    requires:  amount > 0.0 && amount <= balance;
    ensures:   (\result == true  && balance == \old(balance) - amount) ||
               (\result == false && balance == \old(balance));
}

spec transfer {
    signature: boolean transfer(BankAccount target, double amount);
    requires:  target != null && amount > 0.0 && amount <= balance;
    ensures:   (\result == true  && balance == \old(balance) - amount
                                 && target.balance == \old(target.balance) + amount) ||
               (\result == false && balance == \old(balance));
}

spec getBalance {
    signature: double getBalance();
    requires:  true;
    ensures:   \result == balance && \result >= 0.0;
}
