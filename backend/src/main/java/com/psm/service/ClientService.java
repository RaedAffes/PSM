package com.psm.service;

import com.psm.entity.Client;
import com.psm.repository.ClientRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    public Client findById(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> new RuntimeException("Client not found"));
    }

    public Client save(Client client) {
        return clientRepository.save(client);
    }

    public Client update(Long id, Client clientData) {
        Client client = findById(id);
        client.setNom(clientData.getNom());
        client.setTel(clientData.getTel());
        client.setEmail(clientData.getEmail());
        client.setAdresse(clientData.getAdresse());
        return clientRepository.save(client);
    }

    public void delete(Long id) {
        Client client = findById(id);
        clientRepository.delete(client);
    }
}
