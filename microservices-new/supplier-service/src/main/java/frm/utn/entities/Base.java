package frm.utn.entities;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@MappedSuperclass
@Setter
@Getter
public abstract class Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id;
    @Temporal(TemporalType.TIMESTAMP)
    protected Date date = new Date();


}
