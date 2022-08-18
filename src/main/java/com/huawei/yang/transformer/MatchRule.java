package com.huawei.yang.transformer;

import java.util.ArrayList;
import java.util.List;

public class MatchRule {
    private String match;
    private List<String> excepts = new ArrayList<>();

    public MatchRule(String match) {
        this.match = match;
    }

    public MatchRule(String match, List<String> excepts) {
        this.match = match;
        this.excepts = excepts;
    }

    public String getMatch() {
        return match;
    }

    public List<String> getExcepts() {
        return excepts;
    }

    public void addExcept(String except){
        if(excepts.contains(except)){
            return;
        }
        excepts.add(except);
    }
}
