package com.twou.rsvp;

import com.twou.rsvp.model.Rsvp;
import com.twou.rsvp.repository.RsvpRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RsvpRepositoryTests {

    @Autowired
    private RsvpRepository repository;

    @Test
    public void addGetDeleteRsvp() {
        Rsvp rsvp = new Rsvp("John Doe", 2);
        rsvp = repository.save(rsvp);
        Optional<Rsvp> fromDao = repository.findById(rsvp.getRsvpId());
        assertEquals(fromDao.get(), rsvp);
        repository.deleteById(rsvp.getRsvpId());
        fromDao = repository.findById(rsvp.getRsvpId());
        assertFalse(fromDao.isPresent());
    }

    @Test
    public void getAllRsvps() {
        Rsvp rsvp = new Rsvp("Sally Smith", 4);
        repository.save(rsvp);

        rsvp = new Rsvp("George Smith", 3);
        repository.save(rsvp);

        List<Rsvp> rsvps = repository.findAll();

        assertEquals(2, rsvps.size());
    }

    @Test
    public void updateRsvp() {
        Rsvp rsvp = new Rsvp("Joe Jones", 5);
        rsvp = repository.save(rsvp);
        rsvp.setGuestName("NEW NAME");
        repository.save(rsvp);
        Optional<Rsvp> fromDao = repository.findById(rsvp.getRsvpId());
        assertEquals(rsvp, fromDao.get());
    }
}
