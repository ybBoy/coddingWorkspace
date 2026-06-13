package transport;

import application.LedgerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import domain.Budget;
import domain.Expense;
import domain.LedgerConfig;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ledger")
public class LedgerSocket {
    private static final Set<Session> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<Session, YearMonth> sessionMonths = new ConcurrentHashMap<>();
    private static LedgerService ledgerService;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int RECENT_EXPENSES_LIMIT = 20;

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static void setLedgerService(LedgerService service) {
        ledgerService = service;
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        sessionMonths.put(session, YearMonth.now());
        try {
            sendFullState(session, YearMonth.now());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        sessionMonths.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
        sessionMonths.remove(session);
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.path("type").asText();
            JsonNode payload = root.path("payload");

            switch (type) {
                case "ADD_EXPENSE":
                    handleAddExpense(session, payload);
                    break;
                case "DELETE_EXPENSE":
                    handleDeleteExpense(session, payload);
                    break;
                case "SET_BUDGET":
                    handleSetBudget(session, payload);
                    break;
                case "REMOVE_BUDGET":
                    handleRemoveBudget(session, payload);
                    break;
                case "EDIT_EXPENSE":
                    handleEditExpense(session, payload);
                    break;
                case "EXPORT_MONTH":
                    handleExportMonth(session, payload);
                    break;
                case "UPDATE_CONFIG":
                    handleUpdateConfig(session, payload);
                    break;
                case "GET_STATE":
                    YearMonth month = parseYearMonth(payload);
                    sessionMonths.put(session, month);
                    sendFullState(session, month);
                    break;
                default:
                    System.err.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddExpense(Session session, JsonNode payload) {
        BigDecimal amount = new BigDecimal(payload.path("amount").asText());
        String category = payload.path("category").asText();
        String payer = payload.path("payer").asText();
        String remark = payload.path("remark").asText();
        LocalDateTime time = LocalDateTime.parse(
                payload.path("time").asText(),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );

        Expense expense = new Expense(amount, category, payer, remark, time);
        ledgerService.addExpense(expense);
        YearMonth targetMonth = parseYearMonth(payload);
        sessionMonths.put(session, targetMonth);
        broadcastFullState();
    }

    private void handleDeleteExpense(Session session, JsonNode payload) {
        String id = payload.path("id").asText();
        ledgerService.deleteExpense(id);
        YearMonth targetMonth = parseYearMonth(payload);
        sessionMonths.put(session, targetMonth);
        broadcastFullState();
    }

    private void handleSetBudget(Session session, JsonNode payload) {
        String category = payload.path("category").asText();
        BigDecimal amount = new BigDecimal(payload.path("amount").asText());
        ledgerService.setBudget(new Budget(category, amount));
        YearMonth targetMonth = parseYearMonth(payload);
        sessionMonths.put(session, targetMonth);
        broadcastFullState();
    }

    private void handleRemoveBudget(Session session, JsonNode payload) {
        String category = payload.path("category").asText();
        ledgerService.removeBudget(category);
        YearMonth targetMonth = parseYearMonth(payload);
        sessionMonths.put(session, targetMonth);
        broadcastFullState();
    }

    private void handleEditExpense(Session session, JsonNode payload) {
        String id = payload.path("id").asText();
        BigDecimal amount = new BigDecimal(payload.path("amount").asText());
        String category = payload.path("category").asText();
        String payer = payload.path("payer").asText();
        String remark = payload.path("remark").asText();
        String time = payload.path("time").asText();
        ledgerService.editExpense(id, amount, category, payer, remark, time);
        YearMonth targetMonth = parseYearMonth(payload);
        sessionMonths.put(session, targetMonth);
        broadcastFullState();
    }

    private void handleUpdateConfig(Session session, JsonNode payload) {
        String ledgerName = payload.path("ledgerName").asText();
        java.util.List<String> categories = new java.util.ArrayList<>();
        JsonNode catNode = payload.path("categories");
        if (catNode.isArray()) {
            for (JsonNode node : catNode) {
                categories.add(node.asText());
            }
        }
        java.util.List<String> payers = new java.util.ArrayList<>();
        JsonNode payerNode = payload.path("payers");
        if (payerNode.isArray()) {
            for (JsonNode node : payerNode) {
                payers.add(node.asText());
            }
        }
        ledgerService.updateConfig(new LedgerConfig(ledgerName, categories, payers));
        YearMonth targetMonth = parseYearMonth(payload);
        sessionMonths.put(session, targetMonth);
        broadcastFullState();
    }

    private void handleExportMonth(Session session, JsonNode payload) throws IOException {
        YearMonth month = parseYearMonth(payload);
        String format = payload.path("format").asText("csv");

        java.util.List<Expense> expenses = ledgerService.getExpensesByMonth(month);
        java.util.Map<String, BigDecimal> categoryTotals = ledgerService.getCategoryTotals(month);
        java.util.Map<String, BigDecimal> payerTotals = ledgerService.getPayerTotals(month);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("year", month.getYear());
        result.put("month", month.getMonthValue());

        if ("json".equalsIgnoreCase(format)) {
            ArrayNode expArray = objectMapper.createArrayNode();
            for (Expense e : expenses) {
                ObjectNode exp = objectMapper.createObjectNode();
                exp.put("id", e.getId());
                exp.put("amount", e.getAmount().toString());
                exp.put("category", e.getCategory());
                exp.put("payer", e.getPayer());
                exp.put("remark", e.getRemark());
                exp.put("time", e.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                expArray.add(exp);
            }
            result.set("expenses", expArray);

            ObjectNode catStats = objectMapper.createObjectNode();
            categoryTotals.forEach((k, v) -> catStats.put(k, v.toString()));
            result.set("categoryStats", catStats);

            ObjectNode payerStats = objectMapper.createObjectNode();
            payerTotals.forEach((k, v) -> payerStats.put(k, v.toString()));
            result.set("payerStats", payerStats);

            result.put("format", "json");
        } else {
            StringBuilder csv = new StringBuilder();
            csv.append("时间,分类,付款人,金额,备注\n");
            for (Expense e : expenses) {
                csv.append(e.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(",");
                csv.append(escapeCsv(e.getCategory())).append(",");
                csv.append(escapeCsv(e.getPayer())).append(",");
                csv.append(e.getAmount()).append(",");
                csv.append(escapeCsv(e.getRemark())).append("\n");
            }
            csv.append("\n");
            csv.append("分类汇总\n").append("分类,金额,占比\n");
            BigDecimal total = ledgerService.getMonthlyTotal(month);
            for (java.util.Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
                csv.append(escapeCsv(entry.getKey())).append(",");
                csv.append(entry.getValue()).append(",");
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    double pct = entry.getValue().multiply(new BigDecimal("100")).divide(total, 2, java.math.RoundingMode.HALF_UP).doubleValue();
                    csv.append(pct).append("%");
                } else {
                    csv.append("0%");
                }
                csv.append("\n");
            }
            csv.append("\n");
            csv.append("成员汇总\n").append("付款人,金额,占比\n");
            for (java.util.Map.Entry<String, BigDecimal> entry : payerTotals.entrySet()) {
                csv.append(escapeCsv(entry.getKey())).append(",");
                csv.append(entry.getValue()).append(",");
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    double pct = entry.getValue().multiply(new BigDecimal("100")).divide(total, 2, java.math.RoundingMode.HALF_UP).doubleValue();
                    csv.append(pct).append("%");
                } else {
                    csv.append("0%");
                }
                csv.append("\n");
            }
            csv.append("\n").append("本月合计,").append(total.toString());
            result.put("content", csv.toString());
            result.put("format", "csv");
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "EXPORT_RESULT");
        response.set("payload", result);
        session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private YearMonth parseYearMonth(JsonNode payload) {
        if (payload.has("year") && payload.has("month")) {
            int year = payload.path("year").asInt();
            int month = payload.path("month").asInt();
            return YearMonth.of(year, month);
        }
        return YearMonth.now();
    }

    private void sendFullState(Session session, YearMonth month) throws IOException {
        ObjectNode state = buildFullState(month);
        session.getBasicRemote().sendText(objectMapper.writeValueAsString(state));
    }

    private void broadcastFullState() {
        try {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    YearMonth month = sessionMonths.getOrDefault(session, YearMonth.now());
                    String json = objectMapper.writeValueAsString(buildFullState(month));
                    session.getBasicRemote().sendText(json);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ObjectNode buildFullState(YearMonth month) {
        ObjectNode state = objectMapper.createObjectNode();

        state.put("year", month.getYear());
        state.put("month", month.getMonthValue());

        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("total", ledgerService.getMonthlyTotal(month).toString());
        state.set("summary", summary);

        ArrayNode categoryStats = objectMapper.createArrayNode();
        ledgerService.getCategoryTotalsWithBudgets(month).forEach((category, spent) -> {
            ObjectNode cat = objectMapper.createObjectNode();
            BigDecimal budget = ledgerService.getBudgetForCategory(category);
            cat.put("category", category);
            cat.put("spent", spent.toString());
            cat.put("budget", budget.toString());
            categoryStats.add(cat);
        });
        state.set("categoryStats", categoryStats);

        ArrayNode recentExpenses = objectMapper.createArrayNode();
        for (Expense e : ledgerService.getRecentExpenses(RECENT_EXPENSES_LIMIT)) {
            ObjectNode exp = objectMapper.createObjectNode();
            exp.put("id", e.getId());
            exp.put("amount", e.getAmount().toString());
            exp.put("category", e.getCategory());
            exp.put("payer", e.getPayer());
            exp.put("remark", e.getRemark());
            exp.put("time", e.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            recentExpenses.add(exp);
        }
        state.set("recentExpenses", recentExpenses);

        ArrayNode monthExpenses = objectMapper.createArrayNode();
        for (Expense e : ledgerService.getExpensesByMonth(month)) {
            ObjectNode exp = objectMapper.createObjectNode();
            exp.put("id", e.getId());
            exp.put("amount", e.getAmount().toString());
            exp.put("category", e.getCategory());
            exp.put("payer", e.getPayer());
            exp.put("remark", e.getRemark());
            exp.put("time", e.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            monthExpenses.add(exp);
        }
        state.set("monthExpenses", monthExpenses);

        ArrayNode budgets = objectMapper.createArrayNode();
        for (Budget b : ledgerService.getAllBudgets()) {
            ObjectNode bud = objectMapper.createObjectNode();
            bud.put("category", b.getCategory());
            bud.put("amount", b.getAmount().toString());
            budgets.add(bud);
        }
        state.set("budgets", budgets);

        ArrayNode payerStats = objectMapper.createArrayNode();
        ledgerService.getPayerTotals(month).forEach((payer, amount) -> {
            ObjectNode p = objectMapper.createObjectNode();
            p.put("payer", payer);
            p.put("amount", amount.toString());
            payerStats.add(p);
        });
        state.set("payerStats", payerStats);

        ArrayNode warningCategories = objectMapper.createArrayNode();
        ledgerService.getCategoryTotalsWithBudgets(month).forEach((category, spent) -> {
            BigDecimal budget = ledgerService.getBudgetForCategory(category);
            if (budget.compareTo(BigDecimal.ZERO) > 0) {
                double ratio = spent.multiply(new BigDecimal("100")).divide(budget, 4, java.math.RoundingMode.HALF_UP).doubleValue();
                if (ratio >= 80) {
                    ObjectNode wc = objectMapper.createObjectNode();
                    wc.put("category", category);
                    wc.put("ratio", ratio);
                    wc.put("level", ratio >= 100 ? "over" : "warning");
                    warningCategories.add(wc);
                }
            }
        });
        state.set("warningCategories", warningCategories);

        LedgerConfig cfg = ledgerService.getConfig();
        ObjectNode cfgNode = objectMapper.createObjectNode();
        cfgNode.put("ledgerName", cfg.getLedgerName());
        ArrayNode catArray = objectMapper.createArrayNode();
        for (String c : cfg.getCategories()) catArray.add(c);
        cfgNode.set("categories", catArray);
        ArrayNode payerArray = objectMapper.createArrayNode();
        for (String p : cfg.getPayers()) payerArray.add(p);
        cfgNode.set("payers", payerArray);
        state.set("config", cfgNode);

        return state;
    }
}
