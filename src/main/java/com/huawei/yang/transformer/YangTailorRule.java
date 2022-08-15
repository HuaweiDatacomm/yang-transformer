package com.huawei.yang.transformer;

import java.util.ArrayList;
import java.util.List;

public class YangTailorRule {
    private TailorType tailorType;
    private List<String> matches= new ArrayList<>();
    private List<String> noMatches = new ArrayList<>();

    public YangTailorRule(TailorType tailorType, List<String> matches, List<String> noMatches){
        this.tailorType = tailorType;
        this.matches.addAll(matches);
        this.noMatches.addAll(noMatches);
    }

    public TailorType getTailorType() {
        return tailorType;
    }

    public List<String> getMatches() {
        return matches;
    }

    public List<String> getNoMatches() {
        return noMatches;
    }
}
