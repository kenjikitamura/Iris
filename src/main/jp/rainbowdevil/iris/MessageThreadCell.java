package jp.rainbowdevil.iris;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import jp.rainbowdevil.bbslibrary.model.MessageThread;

public class MessageThreadCell extends ListCell<MessageThread>{
	public VBox vBox = new VBox();
	public HBox hbox1 = new HBox();
	public HBox hbox2 = new HBox();
    public Label label = new Label("(empty)");
    public Label label2 = new Label("(empty)");
    public Pane pane = new Pane();
    public MessageThread lastItem;
    
    public MessageThreadCell(){
    	super();
    	vBox.getChildren().addAll(hbox1,hbox2);
    	hbox1.getChildren().addAll(label, pane);
        HBox.setHgrow(pane, Priority.ALWAYS);
        hbox2.getChildren().add(label2);
    }
    
    @Override
    protected void updateItem(MessageThread item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);  // No text in label of super class
        if (empty) {
            lastItem = null;
            setGraphic(null);
        } else {
            lastItem = item;
            label.setText(item!=null ? item.getTitle() : "<null>");
            label2.setText("スレ数:"+item.getSize()+" スレ立て:"+item.getCreatedDate().toString());
            label2.setTextFill(Color.web("#999999"));
            label2.setFont(new Font("ヒラギノ角ゴ Pro W3", 12));
            setGraphic(vBox);
        }
    }
}
