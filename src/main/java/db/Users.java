package db;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "is_admin")
    private boolean is_admin;
    @Column(name = "login")
    private String login;
    @Column(name = "password")
    private String password;
    @Column(name = "created")
    private LocalDate created;
    @OneToOne
    @JoinColumn(name = "table_id", unique = true)
    private Tables tables;
    @ManyToOne
    @JoinColumn(name = "action")
    private Session action;

    public Users() {
    }

    public Users(int id, boolean is_admin, String login, String password, LocalDate created, Tables tables, Session action) {
        this.id = id;
        this.is_admin = is_admin;
        this.login = login;
        this.password = password;
        this.created = created;
        this.tables = tables;
        this.action = action;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isIs_admin() {
        return is_admin;
    }

    public void setIs_admin(boolean is_admin) {
        this.is_admin = is_admin;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public Tables getTables() {
        return tables;
    }

    public void setTables(Tables tables) {
        this.tables = tables;
    }

    public Session getAction() {
        return action;
    }

    public void setAction(Session action) {
        this.action = action;
    }
}
