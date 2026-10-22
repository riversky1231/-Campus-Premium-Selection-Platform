param([string]$BaseUrl='http://localhost:8080', [string]$AdminPassword='AdminDemo123!')
$ErrorActionPreference = 'Stop'
function Invoke-Api([string]$Method,[string]$Path,$Body=$null,[string]$Token='') {
    $args = @{Method=$Method;Uri=($BaseUrl+$Path);ContentType='application/json';TimeoutSec=20}
    if ($Token) { $args.Headers = @{Authorization="Bearer $Token"} }
    if ($null -ne $Body) { $args.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8)) }
    Invoke-RestMethod @args
}
function Assert-Equal($Actual,$Expected,[string]$Description) {
    if ($Actual -ne $Expected) { throw "${Description}：预期 $Expected，实际 $Actual" }
    Write-Host "PASS $Description"
}
$admin = Invoke-Api POST '/api/auth/login' @{username='admin';password=$AdminPassword}
$account = 'smoke_' + [Guid]::NewGuid().ToString('N').Substring(0,16)
$student = Invoke-Api POST '/api/auth/register' @{username=$account;password='SmokeStudent123!';nickname='验收同学'}
$studentToken = $student.token
$uid = $student.user.id
$creditId = [Guid]::NewGuid().ToString('N')
$credit = @{requestId=$creditId;userId=$uid;amount=100}
$null = Invoke-Api POST '/api/admin/wallet/recharge' $credit $admin.token
$null = Invoke-Api POST '/api/admin/wallet/recharge' $credit $admin.token
$balance = Invoke-Api GET '/api/users/wallet' $null $studentToken
Assert-Equal $balance.balance 100 '充值幂等'
$address = Invoke-Api POST '/api/users/addresses' @{recipient='验收同学';phone='13800138000';detail='校园一号自提点'} $studentToken
# Create a dedicated product to avoid modifying another demo's goods.
$categories = Invoke-Api GET '/api/categories'
$product = Invoke-Api POST '/api/admin/products' @{categoryId=$categories[0].id;name=('验收商品-'+$account);description='自动验收专用';price=10;stock=5;onSale=$true} $admin.token
$null = Invoke-Api PUT "/api/cart/$($product.id)" @{quantity=2} $studentToken
$cart = Invoke-Api GET '/api/cart' $null $studentToken
Assert-Equal @($cart).Count 1 '购物车保存'
$request = @{requestId=[Guid]::NewGuid().ToString('N');addressId=$address.id;items=@(@{productId=$product.id;quantity=2})}
$order = Invoke-Api POST '/api/orders' $request $studentToken
$repeat = Invoke-Api POST '/api/orders' $request $studentToken
Assert-Equal $repeat.id $order.id '下单幂等'
$paid = Invoke-Api POST "/api/orders/$($order.id)/pay" $null $studentToken
$null = Invoke-Api POST "/api/orders/$($order.id)/pay" $null $studentToken
$balance = Invoke-Api GET '/api/users/wallet' $null $studentToken
Assert-Equal $balance.balance 80 '重复支付只扣款一次'
$completed = Invoke-Api POST '/api/admin/orders/redeem' @{pickupCode=$paid.pickupCode} $admin.token
Assert-Equal $completed.status 'COMPLETED' '提货核销'
$cancel = Invoke-Api POST '/api/orders' @{requestId=[Guid]::NewGuid().ToString('N');addressId=$address.id;items=@(@{productId=$product.id;quantity=1})} $studentToken
$null = Invoke-Api POST "/api/orders/$($cancel.id)/cancel" $null $studentToken
$null = Invoke-Api POST "/api/orders/$($cancel.id)/cancel" $null $studentToken
$current = Invoke-Api GET "/api/products/$($product.id)"
Assert-Equal $current.stock 3 '取消订单库存恢复一次'
$null = Invoke-Api DELETE '/api/cart' $null $studentToken
$null = Invoke-Api POST '/api/auth/logout' $null $studentToken
Write-Host '业务验收通过。保留验收账号、订单和商品便于复核；不自动删除数据。'
