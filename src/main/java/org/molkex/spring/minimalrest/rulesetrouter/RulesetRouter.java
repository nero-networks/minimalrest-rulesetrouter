package org.molkex.spring.minimalrest.rulesetrouter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.molkex.spring.minimalrest.MinimalMessageBroker;
import org.molkex.spring.minimalrest.MinimalRestApp;
import org.molkex.spring.minimalrest.MinimalStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;

@EnableWebSocket
@RestController
@RequestMapping("/")
public class RulesetRouter extends MinimalMessageBroker {
    static final String RULES = "/rules";

    private MinimalStorage<Ruleset> rulesets = new MinimalStorage<>("rulesets", Ruleset.class);

    @PostMapping("/deliver")
    public void deliver(@RequestBody String payload) {
        ReadContext msg = JsonPath.parse(payload);
        rulesets.values()
                .parallelStream()
                .filter(set -> set.accept(msg))
                .forEach(set -> publish(set.topic, payload));
    }

    @GetMapping(RULES + "/**")
    public String getRules(@ApiIgnore HttpServletRequest request) {
        return getRuleset(request).rules;
    }

    @PutMapping(RULES + "/**")
    public void putRules(@RequestBody String rules, @ApiIgnore HttpServletRequest request) {
        getRuleset(request).rules(rules);
    }

    private Ruleset getRuleset(HttpServletRequest request) {
        return rulesets.computeIfAbsent(request.getRequestURI().substring(RULES.length()), Ruleset::new);
    }

    static class Ruleset extends MinimalStorage.Entity {
        static final String DEFAULT_RULES = "[?($.origin != \"%s\")]";
        static final String RCPT_RULES = "[?(!$.rcpt || ($.rcpt =~ /^%s(\\/.*)?$/))]";

        public String topic;
        public String rules;
        @JsonIgnore String rcptRules;
        @JsonIgnore String path;

        Ruleset() {}

        Ruleset(String topic) {
            this.topic = topic;
            rules(String.format(DEFAULT_RULES, topic));
        }

        void rules(String rules) {
            this.rules = rules;
            init();
            commit();
        }

        boolean accept(ReadContext msg) {
            try {
                if (((JSONArray) msg.read(rcptRules)).size() > 0) {
                    return ((JSONArray) msg.read(path)).size() > 0;
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            return false;
        }

        protected void init() {
            path = rules.replaceAll("\n", "");
            rcptRules = String.format(RCPT_RULES, topic.replaceAll("/", "\\\\/"));
        }
    }

    @SpringBootApplication
    public static class App extends MinimalRestApp {
        public static void main(String[] args) {
            SpringApplication.run(App.class, args);
        }
    }
}
