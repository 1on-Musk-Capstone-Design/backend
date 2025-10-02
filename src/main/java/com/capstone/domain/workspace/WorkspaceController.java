package com.capstone.domain.workspace;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspaces")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceDtos.ListItem>> getAllWorkspaces() {
        List<Workspace> workspaces = workspaceService.getAllWorkspaces();
        
        List<WorkspaceDtos.ListItem> response = workspaces.stream()
                .map(workspace -> {
                    WorkspaceDtos.ListItem item = new WorkspaceDtos.ListItem();
                    item.setWorkspaceId(workspace.getId());
                    item.setName(workspace.getName());
                    return item;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceDtos.ListItem> getWorkspaceById(@PathVariable Long id) {
        try {
            Workspace workspace = workspaceService.getWorkspaceById(id);
            
            WorkspaceDtos.ListItem response = new WorkspaceDtos.ListItem();
            response.setWorkspaceId(workspace.getId());
            response.setName(workspace.getName());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<WorkspaceDtos.Response> create(@RequestBody WorkspaceDtos.CreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Workspace saved = workspaceService.createWorkspace(request.getName().trim());

        WorkspaceDtos.Response resp = new WorkspaceDtos.Response();
        resp.setWorkspaceId(saved.getId());
        resp.setName(saved.getName());
        resp.setCreatedAt(saved.getCreatedAt());

        return ResponseEntity.ok(resp);
    }
}
