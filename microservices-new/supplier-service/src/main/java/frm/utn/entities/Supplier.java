package frm.utn.entities;


import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
@Entity
@Table(name="t_supplier")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier extends Base implements Serializable {
    private String name;
    private  String email;


}