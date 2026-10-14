-- Deliberately invalid statements: these must show type safety errors.

---@shape ValidationPoint
---@field x number
---@field y number

---@type ValidationPoint
local point = { x = "wrong", y = 20 } -- x requires number

---@param count number
local function takesNumber(count)
    print(count)
end

takesNumber("wrong") -- argument requires number
print(point.missing) -- no such member

---@type fun(optional?: string, required: number)
local invalidParameterOrder -- required parameter after optional parameter
