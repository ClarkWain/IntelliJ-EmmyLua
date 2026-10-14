# Flow-sensitive types

[English](flow-types.md) | [简体中文](flow-types.zh-CN.md)

Lua Insight uses conditions at an expression's evaluation point to narrow
optional types. No explicit `@not nil` cast is needed for these guards:

```lua
---@param info Info|nil
local function location(info)
    return info and info.loc ~= 0 and info.loc or nil
end
```

The right operand of `and` is evaluated only when its left operand is truthy.
The right operand of `or` is evaluated only when its left operand is falsy.
Truthiness excludes both `nil` and `false`; `info ~= nil` excludes only `nil`.

Supported guards include parentheses, `not`, equality/inequality with `nil` or
boolean literals, `if/elseif/else` branches and `while` conditions. Facts apply
to local variables, parameters and named members (including literal string keys).
They feed normal type inference, including member checks and argument checks.

A definite non-null assignment also narrows an optional variable or member:

```lua
---@param view View
---@param params LayoutParams
local function update(view, params)
    view.fixedParams = params:clone() -- clone returns LayoutParams
    view.fixedParams.isFixed = true
end
```

The declared field type can remain `LayoutParams|nil`. A nullable return from
`clone()` still needs a check.

Facts are conservative across calls, assignments and loop iterations. They do
not carry into a nested function body or out of a conditional assignment.
Calls can change captured variables and table members, so an earlier check may
need to be repeated after a call. Alias propagation, dynamic/numeric index
narrowing and early-return guard propagation are not currently implemented.
Explicit `@not` casts remain available for facts that cannot be inferred.
