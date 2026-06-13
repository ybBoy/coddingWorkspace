package transport;

import application.LedgerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import domain.Budget;
import domain.Expense;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ledger")
public class LedgerSocket {
    private static final Set<Session> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
        try {
            sendFullState(session, YearMonth.now());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
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
                    handleAddExpense(payload);
                    break;
                case "DELETE_EXPENSE":
                    handleDeleteExpense(payload);
                    break;
                case "SET_BUDGET":
                    handleSetBudget(payload);
                    break;
                case "REMOVE_BUDGET":
                    handleRemoveBudget(payload);
                    break;
                case "GET_STATE":
                    YearMonth month = parseYearMonth(payload);
                    sendFullState(session, month);
                    break;
                default:
                    System.err.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddExpense(JsonNode payload) {
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
        broadcastFullState();
    }

    private void handleDeleteExpense(JsonNode payload) {
        String id = payload.path("id").asText();
        ledgerService.deleteExpense(id);
        broadcastFullState();
    }

    private void handleSetBudget(JsonNode payload) {
        String category = payload.path("category").asText();
        BigDecimal amount = new BigDecimal(payload.path("amount").asText());
        ledgerService.setBudget(new Budget(category, amount));
        broadcastFullState();
    }

    private void handleRemoveBudget(JsonNode payload) {
        String category = payload.path("category").asText();
        ledgerService.removeBudget(category);
        broadcastFullState();
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
            String json = objectMapper.writeValueAsString(buildFullState(YearMonth.now()));
            for (Session session : sessions) {
                if (session.isOpen()) {
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

        return state;
    }
}
