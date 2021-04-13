package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.NumberStatisticModel;
import lombok.extern.log4j.Log4j2;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.markers.Circle;
import org.knowm.xchart.style.markers.Marker;
import org.knowm.xchart.style.markers.None;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Log4j2
@Service
public class ChartService implements BiFunction<List<NumberStatisticModel>, Integer, Optional<String>> {
    private static final String CHART_PATH = "/tmp/chart";
    public static final String CHART_FILE_PATH = CHART_PATH + ".jpg";
    public static final String DEFAULT = "DEFAULT";
    public static final String COMPARE = "COMPARE";

    @Autowired
    private ConfigService configService;

    @Override
    public Optional<String> apply(List<NumberStatisticModel> statisticList, Integer redLine) {
        if(statisticList.isEmpty()) {
            return Optional.empty();
        }

        Consumer<BiConsumer<List<Date>, List<Integer>>> seriesDataProvider = generateSeriesData(statisticList);

        XYChart chart = createChart(seriesDataProvider, redLine);

        try {
            BitmapEncoder.saveBitmapWithDPI(chart, CHART_PATH, BitmapEncoder.BitmapFormat.JPG, 300);
            return Optional.of(CHART_FILE_PATH);
        } catch (Exception e) {
            log.error("Error while create chart", e);
            return Optional.empty();
        }
    }

    private Consumer<BiConsumer<List<Date>, List<Integer>>> generateSeriesData(Iterable<NumberStatisticModel> numberStatisticModels) {
        List<Date> xDataList = new ArrayList<>();
        List<Integer> yDataList = new ArrayList<>();

        for (NumberStatisticModel statistic : numberStatisticModels) {
            LocalDateTime localDateTime = statistic.getDate().toLocalDateTime();
            Timestamp timestamp = Timestamp.valueOf(localDateTime);

            xDataList.add(timestamp);
            yDataList.add(statistic.getValue());
        }

        return consumer -> consumer.accept(xDataList, yDataList);
    }

    private XYChart createChart(Consumer<BiConsumer<List<Date>, List<Integer>>> seriesDataProvider, Integer redLine) {
        ConfigModel config = configService.getConfig();
        BudgetConfigModel budgetConfig = config.getBudget();

        XYChart chart = new XYChartBuilder()
                .width(budgetConfig.getChartWidth())
                .height(budgetConfig.getChartWidth())
                .theme(Styler.ChartTheme.Matlab)
                .build();

        XYStyler styler = chart.getStyler();

        int defaultMaxValue = budgetConfig.getChartDefaultMaxValue();
        int defaultMinValue = budgetConfig.getChartDefaultMinValue();

        seriesDataProvider.accept((dates, integers) -> {
            int maxValue = integers.stream().max(Integer::compareTo).get();

            if(maxValue > defaultMaxValue) {
                styler.setYAxisMax((double) maxValue);
            } else {
                styler.setYAxisMax((double) defaultMaxValue);
            }

            int minValue = integers.stream().min(Integer::compareTo).get();

            if(minValue < defaultMinValue) {
                styler.setYAxisMin((double) minValue);
            } else {
                styler.setYAxisMin((double) defaultMinValue);
            }
        });

        styler.setLegendVisible(false);
        styler.setChartTitleVisible(false);
        styler.setMarkerSize(6);

        chart.setTitle(DEFAULT);
        seriesDataProvider.accept((dates, integers) -> chart.addSeries(DEFAULT, dates, integers));

        List<Marker> markers = new ArrayList<>();
        markers.add(new Circle());

        List<Color> colors = new ArrayList<>();
        colors.add(new Color(25, 128, 25, 255));

        seriesDataProvider.accept((dates, integers) -> {
            if(dates.size() > 1) {
                chart.addSeries(COMPARE, getDateBorderValues(dates), Arrays.asList(redLine, redLine));
                markers.add(new None());
                colors.add(new Color(255, 0, 0, 150));
            }
        });

        styler.setSeriesMarkers(markers.toArray(new Marker[0]));
        styler.setSeriesColors(colors.toArray(new Color[0]));

        return chart;
    }

    private List<Date> getDateBorderValues(List<Date> list) {
        if(list.size() == 2) {
            return list;
        }

        return Arrays.asList(list.get(0), list.get(list.size() - 1));
    }
}
