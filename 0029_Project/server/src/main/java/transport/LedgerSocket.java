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
            sendFullState(session);
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
                case "GET_STATE":
                    sendFullState(session);
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

    private void sendFullState(Session session) throws IOException {
        ObjectNode state = buildFullState();
        session.getBasicRemote().sendText(objectMapper.writeValueAsString(state));
    }

    private void broadcastFullState() {
        try {
            ObjectNode state = buildFullState();
            String json = objectMapper.writeValueAsString(state);
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(json);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ObjectNode buildFullState() {
        YearMonth currentMonth = YearMonth.now();

        ObjectNode state = objectMapper.createObjectNode();

        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("total", ledgerService.getMonthlyTotal(currentMonth).toString());
        state.set("summary", summary);

        ArrayNode categoryStats = objectMapper.createArrayNode();
        ledgerService.getCategoryTotals(currentMonth).forEach((category, spent) -> {
            ObjectNode cat = objectMapper.createObjectNode();
            BigDecimal budget = ledgerService.getBudgetForCategory(category);
            cat.put("category", category);
            cat.put("spent", spent.toString());
            cat.put("budget", budget.toString());
            categoryStats.add(cat);
        });
        state.set("categoryStats", categoryStats);

        ArrayNode expenses = objectMapper.createArrayNode();
        for (Expense e : ledgerService.getExpensesByMonth(currentMonth)) {
            ObjectNode exp = objectMapper.createObjectNode();
            exp.put("id", e.getId());
            exp.put("amount", e.getAmount().toString());
            exp.put("category", e.getCategory());
            exp.put("payer", e.getPayer());
            exp.put("remark", e.getRemark());
            exp.put("time", e.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            expenses.add(exp);
        }
        state.set("expenses", expenses);

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
