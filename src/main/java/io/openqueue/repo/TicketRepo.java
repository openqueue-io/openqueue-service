package io.openqueue.repo;

import io.openqueue.common.constant.LuaScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;

/**
 * @author chenjing
 */
@Repository
public class TicketRepo {

    @Autowired
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    private Map<LuaScript, DefaultRedisScript<List>> redisScriptMap;

    @PostConstruct
    public void init() {
        redisScriptMap = new HashMap<>();
        redisScriptMap.put(LuaScript.TICKET_APPLY, loadScript("scripts/apply_ticket.lua"));
        redisScriptMap.put(LuaScript.TICKET_VERIFY, loadScript("scripts/verify_ticket.lua"));
        redisScriptMap.put(LuaScript.TICKET_ACTIVATE, loadScript("scripts/activate_ticket.lua"));
        redisScriptMap.put(LuaScript.TICKET_REVOKE, loadScript("scripts/revoke_ticket.lua"));
    }

    private DefaultRedisScript<List> loadScript(String luaPath) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(luaPath)));
        return script;
    }

    public Mono<Long> invokeLuaScript(LuaScript luaScript, List<String> keys, List<String> args) {
        return reactiveRedisTemplate
                .execute(redisScriptMap.get(luaScript), keys, args)
                .flatMap(list -> Mono.just((Long) list.get(0)))
                .single();
    }
}
