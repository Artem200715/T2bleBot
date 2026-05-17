package db;

import jakarta.persistence.*;


@Entity
@Table(name = "tables")
public class Tables {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "number")
    private String number;
    @Column(name = "floor")
    private int floor;
    @OneToOne(mappedBy = "table")
    private Users users;

    public Tables() {
    }

    public Tables(int id, String number, int floor, Users users) {
        this.id = id;
        this.number = number;
        this.floor = floor;
        this.users = users;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }
}
