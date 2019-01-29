package io.yggdrash.core.types;

import io.yggdrash.core.types.enumeration.SerialEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class ProposeValidatorSet implements Serializable {
    private static final long serialVersionUID = SerialEnum.PROPOSE_VALIDATOR_SET.toValue();

    private Map<String, Votable> validatorMap;

    public ProposeValidatorSet() {
        validatorMap = new HashMap<>();
    }

    @Data
    public static class Votable implements Serializable {
        private String proposalValidatorAddr;

        private int totalVotableCnt;
        private int agreeCnt;
        private int disagreeCnt;
        private Map<String, Vote> votedMap;

        public Votable(String proposalValidatorAddr, ValidatorSet validatorSet) {
            this.proposalValidatorAddr = proposalValidatorAddr;
            Map<String, Validator> validatorMap = validatorSet.getValidatorMap();
            this.totalVotableCnt = validatorMap.size();
            this.votedMap = new HashMap<>();
            validatorMap.forEach((k, v) -> this.votedMap.put(k, new Vote()));
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Vote implements Serializable {
            boolean isVoted;
            boolean isAgree;
        }

        public enum VoteStatus {
            AGREE,
            DISAGREE,
            NOT_YET
        }

        public VoteStatus status() {
            int cnt = totalVotableCnt;
            if (totalVotableCnt != 2) {
                cnt = (totalVotableCnt / 3) * 2;
                cnt += totalVotableCnt % 3 > 0 ? 1 : 0;
            }

            if (agreeCnt >= cnt) {
                return VoteStatus.AGREE;
            }

            if (disagreeCnt >= cnt) {
                return VoteStatus.DISAGREE;
            }

            if (totalVotableCnt == (agreeCnt + disagreeCnt)) {
                return VoteStatus.DISAGREE;
            }

            return VoteStatus.NOT_YET;
        }
    }
}
