-- Manual verification sample for EmmyLua with Luanalysis.
-- Completion: type a dot after boxed.value to see x and y, both typed number.
-- Documentation: hover Box, Point, callable, or labels.
-- Navigation: Ctrl-click Point or Box in the type annotations.

---@shape Point
---@field x number
---@field y number

---@alias Box<T> { value: T }

---@type Box<Point>
local boxed = { value = { x = 10, y = 20 } }

--- **Squared distance** from the origin. Supports `Point` coordinates.
---@param point Point
---@return number
local function distanceSquared(point)
    return point.x * point.x + point.y * point.y
end

---@class Callable
---@overload fun(value: number): number
local callable = {}

setmetatable(callable, {
    __call = function(_, value)
        return value * 2
    end
})

---@param label? string
---@return string...
local function labels(label)
    return label or "fallback", "extra"
end

---@type number | string
local mixed = 42

local castNumber = --[[---@not string]] mixed
local first, second = labels()

print(distanceSquared(boxed.value), callable(castNumber), first, second)

-- This line is the member-completion probe. Type '.' after the expression.
local completionProbe = boxed.value
