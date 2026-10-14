# 类型收窄

[English](flow-types.md) | 简体中文

Lua Insight 根据表达式执行时已经成立的条件，缩小可选类型的范围。
下面的代码无需手动添加 `@not nil`：

```lua
---@param info Info|nil
local function location(info)
    return info and info.loc ~= 0 and info.loc or nil
end
```

`and` 只有在左侧为真时才计算右侧，`or` 只有在左侧为假时才计算右侧。
真值检查同时排除 `nil` 和 `false`；`info ~= nil` 只排除 `nil`。

支持括号、`not`、与 `nil` 或布尔常量的相等／不等比较，以及 `if/elseif/else`
分支和 `while` 条件。适用于局部变量、参数和具名字段（包括字符串常量索引）。
收窄结果用于正常类型推断，成员检查和参数检查都能使用。

可选变量或字段赋入明确的非空值后，也会收窄：

```lua
---@param view View
---@param params LayoutParams
local function update(view, params)
    view.fixedParams = params:clone() -- clone 返回 LayoutParams
    view.fixedParams.isFixed = true
end
```

字段声明仍可保留 `LayoutParams|nil`。如果 `clone()` 的返回类型包含 nil，仍需判空。

对函数调用、重新赋值和循环迭代采用保守处理。条件不会带入嵌套函数体，某个分支里的
赋值也不会自动应用到分支结束后的代码。函数调用可能修改捕获变量或表字段，因此调用
后可能需要重新判空。目前尚未实现别名传播、动态／数值索引收窄和提前返回后的条件传播。
无法自动推断的条件仍可使用 `@not` 转换表达。
