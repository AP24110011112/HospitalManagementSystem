package hospital.ui;

import hospital.dao.DepartmentDAO;
import hospital.model.Department;
import hospital.service.DepartmentService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.sql.SQLException;

public class DepartmentView extends BorderPane {
    private final DepartmentService service = new DepartmentService();
    private final TableView<Department> table = new TableView<>();
    private final TextField name = new TextField(), description = new TextField();

    public DepartmentView() {
        setPadding(new Insets(26));
        Label title = new Label("Departments"); title.getStyleClass().add("page-title");
        Label subtitle = new Label("Organize doctors by clinical department."); subtitle.getStyleClass().add("muted");
        VBox head = new VBox(5, title, subtitle); head.setPadding(new Insets(0, 0, 18, 0));
        ValidationFeedback.watch(name); ValidationFeedback.watch(description);
        TableColumn<Department,Integer> id = new TableColumn<>("ID"); id.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getId())); id.setPrefWidth(70);
        TableColumn<Department,String> titleCol = new TableColumn<>("Department"); titleCol.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getName()));
        TableColumn<Department,String> desc = new TableColumn<>("Description"); desc.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getDescription()));
        table.getColumns().add(id); table.getColumns().add(titleCol); table.getColumns().add(desc);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().selectedItemProperty().addListener((o, old, d) -> { if (d != null) { name.setText(d.getName()); description.setText(d.getDescription() == null ? "" : d.getDescription()); } });
        VBox form = new VBox(12); form.getStyleClass().add("content-card"); form.setPadding(new Insets(18)); form.setPrefWidth(330);
        Label section = new Label("Department details"); section.getStyleClass().add("section-title");
        name.setPromptText("Department name"); description.setPromptText("Short description");
        Button add = new Button("Add department"); add.getStyleClass().add("primary-button"); add.setMaxWidth(Double.MAX_VALUE); add.setOnAction(e -> save(false));
        Button update = new Button("Save changes"); update.setMaxWidth(Double.MAX_VALUE); update.setOnAction(e -> save(true));
        Button delete = new Button("Delete department"); delete.getStyleClass().add("danger-button"); delete.setMaxWidth(Double.MAX_VALUE); delete.setOnAction(e -> delete());
        Button clear = new Button("Clear"); clear.setMaxWidth(Double.MAX_VALUE); clear.setOnAction(e -> clear());
        form.getChildren().addAll(section, caption("Name", name), caption("Description", description), add, update, delete, clear);
        ScrollPane formScroll = ScrollSupport.vertical(form);
        formScroll.setPrefWidth(350);
        formScroll.setMinWidth(270);
        HBox body = new HBox(18, table, formScroll); HBox.setHgrow(table, Priority.ALWAYS);
        setTop(head); setCenter(body); refresh();
    }
    private VBox caption(String text, Control field) { Label label = new Label(text); label.getStyleClass().add("form-label"); return new VBox(5,label,field); }
    public void refresh() {
        Integer selectedId = table.getSelectionModel().getSelectedItem() == null ? null : table.getSelectionModel().getSelectedItem().getId();
        try {
            table.setItems(FXCollections.observableArrayList(service.getAll()));
            if (selectedId != null) table.getItems().stream().filter(d -> d.getId() == selectedId).findFirst().ifPresent(table.getSelectionModel()::select);
        }
        catch (SQLException ex) { showError("Department list could not be loaded."); }
    }
    private void save(boolean update) {
        Department selected = table.getSelectionModel().getSelectedItem();
        if (update && selected == null) { ValidationFeedback.mark(table); return; }
        try {
            if (update) service.update(selected.getId(), name.getText(), description.getText());
            else service.add(name.getText(), description.getText());
            refresh(); clear(); ToastNotifications.show(this, update ? "Department updated." : "Department added.", ToastNotifications.Kind.SUCCESS);
        } catch (IllegalArgumentException ex) { ValidationFeedback.mark(name); }
        catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) ValidationFeedback.mark(name);
            else showError("Department could not be saved. Check the database connection.");
        }
    }
    private void delete() {
        Department selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { ValidationFeedback.mark(table); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "? Doctors will remain but will no longer belong to a department.", ButtonType.CANCEL, ButtonType.OK);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { service.delete(selected.getId()); refresh(); clear(); ToastNotifications.show(this, "Department deleted.", ToastNotifications.Kind.SUCCESS); }
        catch (SQLException ex) { showError("Department could not be deleted."); }
    }
    private void clear() { table.getSelectionModel().clearSelection(); name.clear(); description.clear(); }
    private void showError(String text) { ToastNotifications.show(this, text, ToastNotifications.Kind.ERROR); }
}
