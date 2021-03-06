package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;

public class DividendTransaction extends Transaction
{
    private Account account;

    private long amount;

    private long totalShares;
    private long fifoCost;

    public DividendTransaction()
    {}

    public Account getAccount()
    {
        return account;
    }

    /* package */void setAccount(Account account)
    {
        this.account = account;
    }

    @Override
    public long getAmount()
    {
        return amount;
    }

    /* package */void setAmount(long amount)
    {
        this.amount = amount;
    }

    public long getDividendPerShare()
    {
        return amountFractionPerShare(amount, getShares());
    }

    public long getFifoCost()
    {
        return fifoCost;
    }

    /* package */void setFifoCost(long fifoCost)
    {
        this.fifoCost = fifoCost;
    }

    /* package */void setTotalShares(long totalShares)
    {
        this.totalShares = totalShares;
    }

    public double getPersonalDividendYield()
    {
        if (fifoCost <= 0)
            return 0;

        double cost = fifoCost;

        if (getShares() > 0)
            cost = fifoCost * (getShares() / (double) totalShares);

        return amount / cost;
    }

    static long amountFractionPerShare(long amount, long shares)
    {
        if (shares == 0)
            return 0;

        return Math.round((double) (amount * (Values.AmountFraction.factor() / Values.Amount.factor()) * Values.Share
                        .divider()) / (double) shares);
    }
}
