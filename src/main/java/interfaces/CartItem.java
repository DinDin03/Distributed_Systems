package interfaces;

import java.io.Serializable;

public class CartItem implements Serializable{
    private String itemName;
    private int quantity;
    private double pricePerUnit;

    public CartItem(String itemName, int quantity, double pricePerUnit){
        this.itemName = itemName;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
    }

    public String getItemName(){
        return itemName;
    }

    public int getQuantity(){
        return quantity;
    }

    public double getPricePerUnit(){
        return pricePerUnit;
    }

    public double totalPrice(){
        return pricePerUnit * quantity;
    }
}
