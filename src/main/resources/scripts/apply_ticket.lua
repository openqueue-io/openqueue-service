---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by jingchen.
--- DateTime: 2020/6/29 9:47 PM
--- Description: Apply a new ticket for that queue.
---

local qid = KEYS[1]

local ticket = cjson.decode(ARGV[1])

local res = redis.call("hget", qid , "id")

-- if queue not exist
if res ~= qid then
    return -1
end

local position = redis.call("hincrby", qid , "tail", 1)

local ticketId = "t:" .. qid .. ":" .. position

redis.call("hmset", ticketId , "id", ticketId, "authCode", ticket.authCode, "issueTime", ticket.issueTime)

return position