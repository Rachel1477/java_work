-- init_root_remote.sql

-- 1. 删除已有的 root@'%'（如果有的话），以免冲突
DROP USER IF EXISTS 'root'@'%';

-- 2. 为 root 创建一个 @'%' 的账号，并指定密码
CREATE USER 'root'@'%' IDENTIFIED BY '123456';

-- 3. 授予该账号所有库的所有权限（开发环境可用）
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

-- 4. 刷新权限表
FLUSH PRIVILEGES;
