---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by jingchen.
--- DateTime: 2020/7/2 1:19 AM
---

local activeSetKey = KEYS[1]
local readySetKey = KEYS[2]
local ticketToken = ARGV[1]
local ticketId = ARGV[2]
local queueId = ARGV[3]
local ticketAuthCode = ARGV[4]

local result = 200

local realAuthCode = redis.call("hget", ticketId, "authCode")
if ticketAuthCode ~= realAuthCode then
    result = 40101
    goto finish
end

local isTicketActivated = redis.call("zscore", activeSetKey, ticketToken)
if isTicketActivated then
    result = 40004
    goto finish
end

local isReadyToActivate = redis.call("zscore", readySetKey, ticketId)
if not isReadyToActivate then
    result = 41202
end

::finish::
local response = {}
response[1] = result

if result ~= 200 then
    return response
end

local expireTime = redis.call("hget", queueId, "availableSecondPerUser")

redis.call("zset", activeSetKey, ticketToken, expireTime)
redis.call("zrem", readySetKey, ticketId)

return response