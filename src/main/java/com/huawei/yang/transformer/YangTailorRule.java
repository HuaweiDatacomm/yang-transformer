package com.huawei.yang.transformer;

import java.util.ArrayList;
import java.util.List;

public class YangTailorRule {
    private TailorType tailorType;
    private List<MatchRule> matches= new ArrayList<>();

    public YangTailorRule(TailorType tailorType, List<MatchRule> matches){
        this.tailorType = tailorType;
        this.matches.addAll(matches);
    }

    public TailorType getTailorType() {
        return tailorType;
    }

    public List<MatchRule> getMatches() {
        return matches;
    }

}
