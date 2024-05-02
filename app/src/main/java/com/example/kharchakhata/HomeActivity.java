package com.example.kharchakhata;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieEntry;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

public class HomeActivity extends AppCompatActivity {
    private static final int READ_SMS_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button readSmsButton = findViewById(R.id.read_sms_button);
        readSmsButton.setOnClickListener(v -> {
            Log.d("ButtonClick", "Button clicked");
            ArrayList<String> smsList = new ArrayList<>();
            ArrayList<ArrayList<String>> processedSmsList = new ArrayList<>();

            EditText startDateEditText = findViewById(R.id.startDateEditText);
            EditText endDateEditText = findViewById(R.id.endDateEditText);
            TextView credit_amount = findViewById(R.id.credit);
            TextView debit_amount = findViewById(R.id.debit);
            TextView totalExpense = findViewById(R.id.total_expense);
            // PieChart pieChart = findViewById(R.id.pieChart);
            BarChart barChart = findViewById(R.id.barChart);

            String startDate = startDateEditText.getText().toString().trim();
            String endDate = endDateEditText.getText().toString().trim();
            double[] expenses = readSms(smsList, processedSmsList, startDate, endDate);
            credit_amount.setText("Credit: INR " + expenses[1]);
            debit_amount.setText("Debit: INR " + expenses[0]);
            if (expenses[2] < 0) {
                totalExpense.setText("Total Savings: INR " + Math.abs(expenses[2]));
            }
            else {
                totalExpense.setText("Total Expense: INR " + expenses[2]);
            }

            // generatePieChart(pieChart, processedSmsList);
            generateBarChart(barChart, processedSmsList);

        });

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        }
    }

    private void generateBarChart(BarChart barChart, ArrayList<ArrayList<String>> processedSmsList) {
        barChart.setBackgroundColor(Color.WHITE);
        barChart.setExtraTopOffset(-30f);
        barChart.setExtraBottomOffset(30f);
        barChart.setExtraLeftOffset(25f);
        barChart.setExtraRightOffset(40f);

        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);

        barChart.getDescription().setEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.LTGRAY);
        xAxis.setTextSize(10f);
        xAxis.setLabelCount(5);
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f);

        YAxis left = barChart.getAxisLeft();
        left.setDrawLabels(false);
        left.setSpaceTop(25f);
        left.setSpaceBottom(25f);
        left.setDrawAxisLine(false);
        left.setDrawGridLines(false);
        left.setDrawZeroLine(true); // draw a zero line
        left.setZeroLineColor(Color.GRAY);
        left.setZeroLineWidth(0.7f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        // Step 1: Calculate Sum of inrValue for Each Category
        HashMap<String, Float> categorySumMap = new HashMap<>();

        for (ArrayList<String> sms : processedSmsList) {
            String category = sms.get(5);
            String transactionType = sms.get(4);
            float inrValue = Float.parseFloat(sms.get(3));
            if (categorySumMap.containsKey(category)) {
                float currentSum = categorySumMap.get(category);
                if (transactionType.equals("Debit")) {
                    categorySumMap.put(category, currentSum + inrValue);
                } else {
                    categorySumMap.put(category, currentSum - inrValue);
                }
            } else {
                categorySumMap.put(category, inrValue);
            }
        }

        // Sort the entries by value in descending order
        List<Map.Entry<String, Float>> sortedEntries = new ArrayList<>(categorySumMap.entrySet());
        sortedEntries.sort((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()));

        // Get the keys with the two maximum float values
        String maxKey1 = sortedEntries.get(0).getKey();
        String maxKey2 = sortedEntries.get(1).getKey();

        TextView suggestions = findViewById(R.id.suggestions);
        String savingSuggestions = "Reduce " + maxKey1 + " expenses\nLimit " + maxKey2 + " spending";
        suggestions.setText(savingSuggestions);

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : categorySumMap.entrySet()) {
            String category = entry.getKey();
            float value = entry.getValue();

            // Add positive and negative values to barEntries
            if (value >= 0) {
                barEntries.add(new BarEntry(index, value));
            } else {
                barEntries.add(new BarEntry(index, 0f));
                barEntries.add(new BarEntry(index, value));
            }

            labels.add(category + " (" + value + ")");
            index++;
        }

        BarDataSet dataSet = new BarDataSet(barEntries, "Expenses");
        dataSet.setColors(Color.BLUE, Color.MAGENTA, Color.GREEN, Color.YELLOW, Color.RED, Color.GRAY, Color.CYAN, Color.WHITE, Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setCenterAxisLabels(true);
        barChart.getXAxis().setAxisMinimum(0f);
        barChart.getXAxis().setAxisMaximum(labels.size());
        barChart.getXAxis().setTextColor(Color.BLACK);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);

        barChart.invalidate();
    }

    private void generatePieChart(PieChart pieChart, ArrayList<ArrayList<String>> processedSmsList) {
        // Step 1: Calculate Sum of inrValue for Each Category
        HashMap<String, Float> categorySumMap = new HashMap<>();

        for (ArrayList<String> sms : processedSmsList) {
            String category = sms.get(5);
            String transactionType = sms.get(4);
            float inrValue = Float.parseFloat(sms.get(3));
            if (categorySumMap.containsKey(category)) {
                float currentSum = categorySumMap.get(category);
                if (transactionType.equals("Debit")) {
                    categorySumMap.put(category, currentSum + inrValue);
                } else {
                    categorySumMap.put(category, currentSum - inrValue);
                }
            } else {
                categorySumMap.put(category, inrValue);
            }
        }

        // Step 2: Prepare Data for Pie Chart
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categorySumMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        // Step 3: Create Pie Chart
        PieDataSet dataSet = new PieDataSet(entries, "Expenses");
        dataSet.setColors(Color.BLUE, Color.MAGENTA, Color.GREEN, Color.YELLOW, Color.RED, Color.GRAY, Color.CYAN, Color.WHITE, Color.BLACK);
        dataSet.setValueTextSize(20f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private double[] readSms(ArrayList<String> smsList, ArrayList<ArrayList<String>> processedSmsList, String startDate, String endDate) {
        Log.d("Inside readSms", "Reading sms");
        Instant startInstant = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE)
                .atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE)
                .atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusSeconds(1).toInstant();

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                Telephony.Sms.DATE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(startInstant.toEpochMilli()), String.valueOf(endInstant.toEpochMilli())},
                Telephony.Sms.DATE + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));

                Instant instant = Instant.ofEpochSecond(Long.parseLong(date.substring(0, date.length() - 3)));
                LocalDateTime gmtDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("IST"));

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String gmtDateString = gmtDateTime.format(formatter);

                if ((address.contains("AXIS") || address.contains("Axis") || address.contains("AU") || address.contains("SBI") || address.contains("SBM")) &&
                        (body.contains("Debit") || body.contains("Spent") || body.contains("credited")) && (!body.contains("Customer,") && !body.contains("Maintenance"))) {
                    smsList.add("Sender: " + address + ";Message: " + body + ";Date: " + gmtDateString);
                }
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return processSms(smsList, processedSmsList);
    }

    private double[] processSms(ArrayList<String> smsList, ArrayList<ArrayList<String>> processedSmsList) {
        double[] values = new double[3];
        double inr = 0.00;
        for (String sms : smsList) {
            String[] parts = sms.split(";");
            String sender = parts[0].substring("Sender: ".length());
            String message = parts[1].substring("Message: ".length()).replace(",", "");
            String date = parts[2].substring("Date: ".length());

            // Extract INR value from the body
            String inrValue = extractINRValue(message);
            try{
                inr = Double.parseDouble(inrValue);
            }
            catch (NumberFormatException e) {
                inr = 0.00;
                System.out.println("Skipping unconvertible inrValue: " + inrValue);
            }

            // Determine debit or credit
            String transactionType = determineTransactionType(message);

            String expenseCategory = determineExpenseCategory(message);

            ArrayList<String> processedSms = new ArrayList<>();
            processedSms.add(sender);
            processedSms.add(date);
            processedSms.add(message);
            processedSms.add(String.valueOf(inr));
            processedSms.add(transactionType);
            processedSms.add(expenseCategory);

            processedSmsList.add(processedSms);

            if (transactionType.equals("Debit")) {
                values[0] += inr;
                values[2] += inr;
            } else if (transactionType.equals("Credit")) {
                values[1] += inr;
                values[2] -= inr;
            }
            // Log.d("Message", message);
            Log.d("INR", inrValue);
            Log.d("Expense Category", expenseCategory);
        }

        return values;
    }

    private String determineExpenseCategory(String message) {
        Map<String, String[]> categoryKeywords = new HashMap<>();
        categoryKeywords.put("bank", new String[]{"cred"});
        categoryKeywords.put("food", new String[]{
                "food", "restaurant", "grocery", "meal", "swiggy", "zomato", "hungerbox"});
        categoryKeywords.put("travel", new String[]{
                "travel", "flight", "hotel", "ticket", "transport", "trip"});
        categoryKeywords.put("medical", new String[]{
                "medical", "health", "doctor", "hospital", "medicine", "pharmacy", "apollo"});
        categoryKeywords.put("investment", new String[]{
                "investment", "stock", "mutual fund", "shares", "portfolio"});
        categoryKeywords.put("entertainment", new String[]{
                "entertainment", "movie", "music", "concert", "event"});
        categoryKeywords.put("utilities", new String[]{
                "utilities", "bill", "payment", "electricity", "water", "gas", "recharg", "prepaid"});
        categoryKeywords.put("others", new String[]{"others", "miscellaneous", "unknown"});

        for (Map.Entry<String, String[]> entry : categoryKeywords.entrySet()) {
            String category = entry.getKey();
            String[] keywords = entry.getValue();
            for (String keyword : keywords) {
                if (message.toLowerCase().contains(keyword.toLowerCase())) {
                    return category;
                }
            }
        }
        return "others";
    }

    private String extractINRValue(String message) {
        // Logic to extract INR value from the message body
        if (message.contains("INR")) {
            int endIndex = -1;
            int startIndex = message.indexOf("INR ") + 4;
            int endIndex1 = message.indexOf("A/c", startIndex);
            int endIndex2 = message.indexOf("credited", startIndex);
            int endIndex3 = message.indexOf("from", startIndex);
            int endIndex4 = message.indexOf("has", startIndex);
            int endIndex5 = message.indexOf("-", startIndex) - 2;
            int endIndex6 = message.indexOf("on", startIndex);
            if (endIndex1 != -1) {
                endIndex = endIndex1;
            }
            if (endIndex2 != -1 && (endIndex1 == -1 || endIndex2 < endIndex1)) {
                endIndex = endIndex2;
            }
            if (endIndex6 != -1 && (endIndex1 == -1 || endIndex6 < endIndex1) && (endIndex2 == -1 || endIndex6 < endIndex2)) {
                endIndex = endIndex6;
            }
            if (endIndex3 != -1 && (endIndex1 == -1 || endIndex3 < endIndex1) && (endIndex2 == -1 || endIndex3 < endIndex2) && (endIndex6 == -1 || endIndex3 < endIndex6)) {
                endIndex = endIndex3;
            }
            if (endIndex4 != -1 && (endIndex1 == -1 || endIndex4 < endIndex1) && (endIndex2 == -1 || endIndex4 < endIndex2) && (endIndex3 == -1 || endIndex4 < endIndex3) && (endIndex6 == -1 || endIndex4 < endIndex6)) {
                endIndex = endIndex4;
            }
            if (endIndex5 != -1 && (endIndex1 == -1 || endIndex5 < endIndex1) && (endIndex2 == -1 || endIndex5 < endIndex2) && (endIndex3 == -1 || endIndex5 < endIndex3) && (endIndex4 == -1 || endIndex5 < endIndex4) && (endIndex6 == -1 || endIndex5 < endIndex6)) {
                endIndex = endIndex5;
            }
            // Log.d("endIndex",String.valueOf(endIndex));
            if (startIndex < endIndex) {
                return message.substring(startIndex, endIndex);
            };
        }
        return "0.00";
    }

    private String determineTransactionType(String message) {
        // Logic to determine debit or credit from the message body
        // Example:
        // You might check for specific keywords like "Debit" or "Credit"
        // in the message body to determine the transaction type
        if (message.contains("Debit") || message.contains("debited") || message.contains("Spent")) {
            return "Debit";
        } else if (message.contains("Credit") || message.contains("credited")) {
            return "Credit";
        }
        return "";
    }
}