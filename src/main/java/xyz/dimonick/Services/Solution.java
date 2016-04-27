package xyz.dimonick.Services;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

public class Solution {

    private static volatile Solution instance;
    private HashMap<YearMonth, BigDecimal> indexes;
    private List<String> BasePerList;
    private List<String> CalcPerList;
    private BigDecimal minzp;
    private YearMonth startIndexesPeriod = YearMonth.of(2000, 01);
    private YearMonth endIndexesPeriod;
    private static final YearMonth startCalc = YearMonth.of(2016,1);
    private BigDecimal newLimit = new BigDecimal("1.03");
    private BigDecimal oldLimit = new BigDecimal("1.01");






    public static Solution getInstance() {
        Solution localInstance = instance;
        if (localInstance == null) {
            synchronized (Solution.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Solution();
                }
            }
        }
        return localInstance;
    }

    private Solution(){
        init();
    }

    void startIndexScheduler(){
        long day = 86400000l;
        Timer time = new Timer();
        DownloadScheduler ds = new DownloadScheduler();
        time.schedule(ds, 0, day);
    }


    public void init(){
        indexes = IndexDownloader.getIdexes();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        startIndexScheduler();
        minzp = new BigDecimal(ReadProperties.getProperty("minimal_salary"));
        fillBasePeriod();
        try {
            fillCalcPeriod();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //fill avalible base period

    public void fillBasePeriod()
    {
        BasePerList = new ArrayList<String>(indexes.size());
        YearMonth tmpPeriod = getStartIndexesPeriod();
        for (int i = 0; i < indexes.size(); i++) {
            BasePerList.add(tmpPeriod.toString());
            tmpPeriod = tmpPeriod.plusMonths(1);
        }
        setEndIndexesPeriod(tmpPeriod.minusMonths(1));
    }

    // fill avalible calculation period

    public void fillCalcPeriod() throws Exception {
        YearMonth tmpPeriod;
        YearMonth maxAvalible = getEndIndexesPeriod().plusMonths(2);
        if (maxAvalible.compareTo(getStartCalc())<0) {
            throw new Exception("Not all indexes are loaded!");
        }
        tmpPeriod = getStartCalc();
        ArrayList<String> list = new ArrayList<String>();
        while(tmpPeriod.compareTo(maxAvalible) <= 0) {
            list.add(tmpPeriod.toString());
            tmpPeriod = tmpPeriod.plusMonths(1);
        }
        CalcPerList = new ArrayList<String>(list.size());
        for(int i=0; i< list.size(); i++){
            CalcPerList.add(list.get(i));
        }

    }

    /**
     * @param basePer base period such as"2007-12", not null
     * @param calcPeriod pay period such as"2007-12", not null
     * @param method true - 103% accept to all indexes, false - accept to all payroll from 01.2016
     * @return indexation coefficient, not null
     */

    public BigDecimal solve (String basePer, String calcPeriod, Boolean method){
        if(basePer == null || calcPeriod == null) {
            throw new IllegalArgumentException("Parameters are incorrect");
        }
        BigDecimal coefficient = BigDecimal.ONE;
        BigDecimal limit = newLimit;
        BigDecimal bound = BigDecimal.ZERO;
        YearMonth base = YearMonth.parse(basePer);
        YearMonth calc = YearMonth.parse(calcPeriod);
        ArrayList<BigDecimal> excessLimit = new ArrayList<BigDecimal>();

        if (base.compareTo(calc.minusMonths(2))>0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        for(YearMonth i = base.plusMonths(1); i.compareTo(calc.minusMonths(1))< 0; i = i.plusMonths(1) ){

            if(indexes.get(i)==null){
                return new BigDecimal("-1");
            }

            if(bound.compareTo(BigDecimal.ZERO)> 0) {
                bound = bound.multiply(indexes.get(i));
            }
            else bound = indexes.get(i);


            if(!method && i.compareTo(startCalc) < 0){
                limit = oldLimit;
            }
            else {
                limit = newLimit;
            }
            if(bound.compareTo(limit)>=0) {
                excessLimit.add(bound.setScale(3, RoundingMode.HALF_UP));
                bound = BigDecimal.ZERO;
            }
        }

        for(BigDecimal count: excessLimit) {
            coefficient = coefficient.multiply(count);
        }

        coefficient = coefficient.subtract(BigDecimal.ONE);
        if (coefficient.compareTo(BigDecimal.ZERO) < 0) {
            coefficient = BigDecimal.ZERO ;
        }
        coefficient = coefficient.setScale(3, RoundingMode.HALF_UP);

        return coefficient;
    }



    public HashMap<YearMonth, BigDecimal> getIndexes() {
        return indexes;
    }

    public void setIndexes(HashMap<YearMonth, BigDecimal> indexes) {
        this.indexes = indexes;
    }

    public List<String> getBasePerList() {
        return BasePerList;
    }

    public void setBasePerList(List<String> basePerList) {
        BasePerList = basePerList;
    }

    public List<String> getCalcPerList() {
        return CalcPerList;
    }

    public void setCalcPerList(List<String> calcPerList) {
        CalcPerList = calcPerList;
    }

    public BigDecimal getMinzp() {
        return minzp;
    }

    public void setMinzp(BigDecimal minzp) {
        this.minzp = minzp;
    }

    public YearMonth getEndIndexesPeriod() {
        return endIndexesPeriod;
    }

    public void setEndIndexesPeriod(YearMonth endIndexesPeriod) {
        this.endIndexesPeriod = endIndexesPeriod;
    }

    public YearMonth getStartIndexesPeriod() {
        return startIndexesPeriod;
    }

    public void setStartIndexesPeriod(YearMonth startIndexesPeriod) {
        this.startIndexesPeriod = startIndexesPeriod;
    }

    public static YearMonth getStartCalc() {
        return startCalc;
    }
}