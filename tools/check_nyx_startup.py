"""Static startup regression guard; this is not an Android device test."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root / 'TMessagesProj/src/main/java/org/telegram'
launch = (source / 'ui/LaunchActivity.java').read_text()
base = (source / 'ui/BasePermissionsActivity.java').read_text()
premium = (source / 'messenger/NyxPremium.java').read_text()
assert 'protected int currentAccount = -1;' in base
start = launch.index('protected void onCreate(')
selected = launch.index('currentAccount = UserConfig.selectedAccount;', start)
observe = launch.index('NyxPremium.observe(currentAccount);', start)
assert selected < observe, 'Premium observer must not see the inherited -1 startup account'
method = premium[premium.index('public static void observe(int account)'):]
assert method.index('!validAccount(account)') < method.index('NotificationCenter.getInstance(account)'), 'Observer must reject invalid account indexes'
assert 'account>=0&&account<UserConfig.MAX_ACCOUNT_COUNT' in premium
print('PASS: Nyxgram startup account selection and observer bounds guards')
