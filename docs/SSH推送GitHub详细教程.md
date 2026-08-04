# 使用 SSH 推送项目到 GitHub 详细教程

> 适用系统：Windows / macOS / Linux | 前提：已安装 Git

---

## 一、检查 SSH 密钥是否存在

打开终端（PowerShell 或 Git Bash），执行：

```bash
ls ~/.ssh/id_rsa.pub
```

- 如果显示文件路径 → 跳至**第三步**
- 如果提示 `No such file or directory` → 执行**第二步**

---

## 二、生成 SSH 密钥

```bash
ssh-keygen -t rsa -b 4096 -C "你的GitHub邮箱@example.com"
```

执行后会提示：

```
Enter file in which to save the key (C:\Users\你的用户名/.ssh/id_rsa):
```

直接按 **回车** 使用默认路径。

```
Enter passphrase (empty for no passphrase):
```

直接按 **回车**（不设密码），再次回车确认。

看到 `Your identification has been saved...` 即生成成功。

---

## 三、复制公钥内容

**Windows PowerShell：**

```powershell
Get-Content ~/.ssh/id_rsa.pub | Set-Clipboard
```

**Git Bash / macOS / Linux：**

```bash
cat ~/.ssh/id_rsa.pub
```

然后手动全选复制输出的全部内容（以 `ssh-rsa` 开头，以邮箱结尾）。

---

## 四、添加 SSH 公钥到 GitHub

1. 浏览器打开 [GitHub SSH Keys 设置](https://github.com/settings/keys)
2. 点击绿色按钮 **New SSH Key**
3. **Title**：填写一个辨识名称，如 `我的台式机`
4. **Key type**：选 `Authentication Key`
5. **Key**：粘贴刚才复制的公钥内容
6. 点击 **Add SSH Key**
7. 输入 GitHub 密码确认

---

## 五、测试 SSH 连接

```bash
ssh -T git@github.com
```

首次连接会提示：

```
The authenticity of host 'github.com' can't be established.
Are you sure you want to continue connecting (yes/no/[fingerprint])?
```

输入 **`yes`** 回车。

如果看到以下信息即连接成功：

```
Hi 你的用户名! You've successfully authenticated, but GitHub does not provide shell access.
```

---

## 六、在 GitHub 创建远程仓库

1. 浏览器打开 [GitHub 新建仓库](https://github.com/new)
2. **Repository name**：填写仓库名，如 `meitan-gas-analyzer`
3. **Description**：可选，填写项目描述
4. **Public / Private**：根据需要选择公开或私有
5. ⚠️ **不要勾选** "Add a README file"、"Add .gitignore"、"Choose a license"
6. 点击 **Create repository**

创建后 GitHub 会显示推送指引，类似：

```
git@github.com:你的用户名/meitan-gas-analyzer.git
```

复制这个 SSH 地址备用。

---

## 七、推送本地仓库到 GitHub

在项目根目录 `meitan/` 下依次执行：

```bash
# 1. 确认在正确的目录
cd D:/TARE-project/meitan-project/meitan

# 2. 添加所有文件到暂存区
git add .

# 3. 首次提交
git commit -m "init: 煤层瓦斯智能分析平台初始版本"

# 4. 添加远程仓库（替换为你的仓库地址）
git remote add origin git@github.com:你的用户名/meitan-gas-analyzer.git

# 5. 推送到 GitHub（首次推送需要 -u 设置上游分支）
git push -u origin master
```

---

## 八、常见问题

### 8.1 `Permission denied (publickey)`

SSH 密钥未添加或未正确添加到 GitHub。回到**第三步和第四步**检查。

### 8.2 `remote origin already exists`

之前已添加过远程仓库，执行以下命令更新地址：

```bash
git remote set-url origin git@github.com:你的用户名/meitan-gas-analyzer.git
```

### 8.3 `failed to push some refs`

远程仓库已有文件（如创建时勾选了 README），先拉取合并：

```bash
git pull origin master --allow-unrelated-histories
git push origin master
```

### 8.4 默认分支名不是 master

部分新版 Git 默认分支名为 `main`，推送时改用：

```bash
git push -u origin main
```

不确定当前分支名时，执行 `git branch` 查看。

---

## 九、后续日常推送流程

```bash
git add .
git commit -m "描述本次改动"
git push
```

首次设置 `-u origin master` 之后，后续直接 `git push` 即可。
