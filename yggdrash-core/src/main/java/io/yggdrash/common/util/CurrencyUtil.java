package io.yggdrash.common.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigInteger;

public class CurrencyUtil {
    private static final String CELL = "1";
    private static final String KCELL = "1000";
    private static final String MCELL = "1000000";
    private static final String GCELL = "1000000000";
    private static final String MICROYEED = "1000000000000";
    private static final String MILLIYEED = "1000000000000000";
    private static final String YEED = "1000000000000000000";

    public enum CurrencyType {
        CELLType(1),
        KCELLType(2),
        MCELLType(3),
        GCELL(4),
        MICROYEEDType(5),
        MILLIYEEDType(6),
        YEEDType(7);

        private int value;

        CurrencyType(int value) {
            this.value = value;
        }

        @JsonCreator
        public static CurrencyType fromValue(int value) {
            switch (value) {
                case 1:
                    return CELLType;
                case 2:
                    return KCELLType;
                case 3:
                    return MCELLType;
                case 4:
                    return GCELL;
                case 5:
                    return MICROYEEDType;
                case 6:
                    return MILLIYEEDType;
                case 7:
                    return YEEDType;
                default:
                    return CELLType;
            }
        }

        @JsonValue
        public int toValue() {
            return this.value;
        }
    }

    public static BigInteger generateCell(CurrencyType currencyType, long amount) {
        return generateCell(currencyType, new BigInteger(String.valueOf(amount)));
    }

    public static BigInteger generateCell(CurrencyType currencyType, BigInteger amount) {
        BigInteger cell = new BigInteger(amount.toString());
        switch (currencyType) {
            case CELLType:
                return cell.multiply(new BigInteger(CELL));
            case KCELLType:
                return cell.multiply(new BigInteger(KCELL));
            case MCELLType:
                return cell.multiply(new BigInteger(MCELL));
            case GCELL:
                return cell.multiply(new BigInteger(GCELL));
            case MICROYEEDType:
                return cell.multiply(new BigInteger(MICROYEED));
            case MILLIYEEDType:
                return cell.multiply(new BigInteger(MILLIYEED));
            case YEEDType:
                return cell.multiply(new BigInteger(YEED));
            default:
                return null;
        }
    }

    public static BigInteger generateCurrencyToAnotherCurrency(CurrencyType sourceType,
                                                               CurrencyType targetType,
                                                               BigInteger sourceAmount) {
        BigInteger defaultAmount = generateCell(sourceType, sourceAmount);
        BigInteger divValue = new BigInteger("1");

        switch (targetType) {
            case CELLType:
                divValue = divValue.multiply(new BigInteger(CELL));
                defaultAmount = defaultAmount.divide(divValue);
                break;
            case KCELLType:
                divValue = divValue.multiply(new BigInteger(KCELL));
                defaultAmount = defaultAmount.divide(divValue);
                break;
            case MCELLType:
                divValue = divValue.multiply(new BigInteger(MCELL));
                defaultAmount = defaultAmount.divide(divValue);
                break;
            case GCELL:
                divValue = divValue.multiply(new BigInteger(GCELL));
                defaultAmount = defaultAmount.divide(divValue);
                break;
            case MICROYEEDType:
                divValue = divValue.multiply(new BigInteger(MICROYEED));
                defaultAmount = defaultAmount.divide(divValue);
                break;
            case MILLIYEEDType:
                divValue = divValue.multiply(new BigInteger(MILLIYEED));
                defaultAmount = defaultAmount.divide(divValue);
                break;
            case YEEDType:
                divValue = divValue.multiply(new BigInteger(YEED));
                defaultAmount = defaultAmount.divide(divValue);
                break;
            default:
                break;
        }
        return defaultAmount;
    }

    public static String generateStringToCurrency(CurrencyType sourceType,
                                                  CurrencyType targetType,
                                                  BigInteger sourceAmount) {
        BigInteger defaultAmount = generateCell(sourceType, sourceAmount);

        String targetAmount = "";
        switch (targetType) {
            case CELLType:
                targetAmount = StringUtil.convertStringFloatingPoint(defaultAmount.toString(), 0);
                break;
            case KCELLType:
                targetAmount = StringUtil.convertStringFloatingPoint(defaultAmount.toString(), 3);
                break;
            case MCELLType:
                targetAmount = StringUtil.convertStringFloatingPoint(defaultAmount.toString(), 6);
                break;
            case GCELL:
                targetAmount = StringUtil.convertStringFloatingPoint(defaultAmount.toString(), 9);
                break;
            case MICROYEEDType:
                targetAmount = StringUtil.convertStringFloatingPoint(defaultAmount.toString(), 12);
                break;
            case MILLIYEEDType:
                targetAmount = StringUtil.convertStringFloatingPoint(defaultAmount.toString(), 15);
                break;
            case YEEDType:
                targetAmount = StringUtil.convertStringFloatingPoint(defaultAmount.toString(), 18);
                break;
            default:
                break;
        }

        return targetAmount;
    }

}
