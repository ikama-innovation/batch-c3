package se.ikama.bauta.ui;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import se.ikama.bauta.core.BautaManager;
import se.ikama.bauta.core.JobEventListener;
import se.ikama.bauta.core.JobInstanceInfo;
import se.ikama.bauta.core.StepInfo;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Push
@Route("")
//@Theme(value = Material.class, variant = Material.LIGHT)
@Theme(value = Lumo.class, variant = Lumo.DARK)
// @PWA(name = "Project Base for Vaadin Flow with Spring", shortName = "Project Base")
@CssImport(value = "./styles/job-grid-theme.css", themeFor = "vaadin-grid")
@CssImport(value = "./styles/bauta-styles.css")
@Viewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes, viewport-fit=cover")
@DependsOn("bautaManager")
public class MainView extends AppLayout implements JobEventListener {

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    BautaManager bautaManager;

    @Autowired
    SchedulingView schedulingView;

    //private UI ui;
    Grid<JobInstanceInfo> grid = null;
    ArrayList<Button> actionButtons = new ArrayList<>();
    Tabs menuTabs = null;

    public MainView() {
        log.debug("Constructing main view. Hashcode: {}", this.hashCode());

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        log.debug("Attach");
        bautaManager.registerJobChangeListener(this);
    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        log.debug("Detach");
        bautaManager.unregisterJobChangeListener(this);
    }

    @PostConstruct
    @DependsOn("bautaManager")
    public void init() {
        createMainView();
    }

    private void createMainView() {
        log.debug("Creating main view");

        Image img = new Image("../static/images/bauta-logo-light.png", "Bauta logo");
        img.setHeight("28px");
        DrawerToggle drawerToggle = new DrawerToggle();
        this.addToNavbar(new DrawerToggle(), img);
        this.setDrawerOpened(false);

        Map<Tab, Component> tabsToPages = new HashMap<>();
        Component jobPage = createJobView();
        jobPage.setVisible(true);
        // Job tab
        Tab jobTab = new Tab("Jobs");
        Component aboutPage = createAboutView();
        aboutPage.setVisible(false);
        // About tab
        Tab aboutTab = new Tab("About");
        // Scheduling
        Tab schedulingTab = new Tab("Scheduling");


        tabsToPages.put(jobTab, jobPage);
        tabsToPages.put(aboutTab, aboutPage);
        tabsToPages.put(schedulingTab, schedulingView);
        schedulingView.setVisible(false);

        Tabs tabs = new Tabs(jobTab, schedulingTab, aboutTab);
        tabs.setSelectedTab(jobTab);
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        Div pages = new Div(jobPage, schedulingView, aboutPage);
        Set<Component> pagesShown = Stream.of(jobPage)
                .collect(Collectors.toSet());
        tabs.addSelectedChangeListener(event -> {
            pagesShown.forEach(page -> page.setVisible(false));
            pagesShown.clear();
            Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);

            pagesShown.add(selectedPage);
        });
        this.addToDrawer(tabs);
        try {
            this.setContent(pages);
        } catch (Exception e) {
            this.setContent(new Label("Failed to create job view: " + e.getMessage()));
        }
        Div rightPanel = new Div();


        rightPanel.getStyle().set("margin-right", "20px");
            /*Button upgradeBautaButton = new Button("");
            upgradeBautaButton.getElement().setProperty("title", "Upgrades the Bauta framework to the latest version");
            upgradeBautaButton.setIcon(VaadinIcon.POWER_OFF.create());
            //upgradeBautaButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            */

        Button upgradeInstanceButton = new Button("", clickEvent -> {
            try {
                bautaManager.rebuildServer();
                showInfoMessage("Restarting server. Hold on until it is up and running again..");

            } catch (Exception e) {
                showErrorMessage("Failed to rebuild server: " + e.getMessage());
            }
        });
        upgradeInstanceButton.getElement().setProperty("title", "Upgrades this instance by fetching latest scripts and job definitions from VCS");
        upgradeInstanceButton.setIcon(VaadinIcon.REFRESH.create());
        //upgradeInstanceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        upgradeInstanceButton.getStyle().set("margin-right", "5px");

        Label buildInfo = new Label(bautaManager.getShortServerInfo());
        buildInfo.setClassName("build-info");
        //buildInfo.getStyle().set("font-size", "0.75em");
        //buildInfo.getStyle().set("margin-right", "10px");
        rightPanel.add(buildInfo);
        rightPanel.add(upgradeInstanceButton);
        //rightPanel.add(upgradeBautaButton);

        //rightPanel.add(new Button("Another"));

        rightPanel.getStyle().set("margin-left", "auto").set("text-alight", "right");

        this.addToNavbar(rightPanel);

    }

    private Component createAboutView() {
        VerticalLayout aboutView = new VerticalLayout();
        aboutView.setWidthFull();
        UnorderedList ul = new UnorderedList();
        for (String i : bautaManager.getServerInfo()) {
            ul.add(new ListItem(i));
        }
        aboutView.add(ul);
        return aboutView;
    }

    private Component createJobView() {
        VerticalLayout jobView = new VerticalLayout();
        jobView.setPadding(true);
        jobView.setWidthFull();
        //setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        this.grid = new Grid();
        grid.addClassName("jobgrid");
        grid.setId("jobgrid");
        grid.setHeightByRows(true);
        grid.setVerticalScrollingEnabled(false);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setDetailsVisibleOnClick(false);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        Grid.Column<JobInstanceInfo> c = grid.addColumn(item -> item.getName());
        c.setClassNameGenerator(item -> "job_cell");
        //grid.addColumn(item->createJobInfo(grid, item)).setHeader("Status");
        grid.addColumn(TemplateRenderer.<JobInstanceInfo>of(
                "<div class='job-cell-info'>"+
                        "Instance ID: [[item.instanceId]]<br>" +
                        "ExecutionID: [[item.executionId]]<br>" +
                        "Executions: [[item.executionCount]]<br>" +
                        "Started: [[item.startTime]]<br>" +
                        "Ended: [[item.endTime]]<br>" +
                        "Latest Duration: [[item.latestDuration]]<br>" +
                        "Total Duration: [[item.duration]]<br>" +
                        "Exit status: <div class='step_status' data-exitstatus$={{item.exitStatus}}>[[item.exitStatus]]</div><br>" +
                        "Params: [[item.params]]<br>" +
                        "</div>"
                )
                        .withProperty("executionId", JobInstanceInfo::getLatestExecutionId)
                        .withProperty("instanceId", JobInstanceInfo::getInstanceId)
                        .withProperty("exitStatus", JobInstanceInfo::getExitStatus)
                        .withProperty("executionCount", JobInstanceInfo::getExecutionCount)
                        .withProperty("startTime", ji -> ji.getStartTime() != null ? DateFormatUtils.format(ji.getStartTime(), "yyMMdd HH:mm:ss", Locale.US):"-")
                        .withProperty("endTime", ji -> ji.getEndTime() != null ? DateFormatUtils.format(ji.getEndTime(), "yyMMdd HH:mm:ss", Locale.US) : "-")
                        .withProperty("duration", ji -> ji != null ? DurationFormatUtils.formatDuration(ji.getDuration(), "HH:mm:ss") : "")
                        .withProperty("latestDuration", ji -> ji != null ? DurationFormatUtils.formatDuration(ji.getLatestDuration(), "HH:mm:ss") : "")
                        .withProperty("params", ji -> ji.getJobParameters() != null ? ji.getJobParameters().toString() : "")
        );

        grid.addComponentColumn(item -> createStepComponent(grid, item));
        grid.addComponentColumn(item -> createButtons(grid, item));

        //grid.addColumn(item->item.getSteps().toArray().toString()).setHeader("Steps");
        try {
            grid.setItems(bautaManager.jobDetails());
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Failed to fetch job details");
        }
        jobView.add(grid);
        return jobView;
    }
    private void doStartJob(JobInstanceInfo item, Map<String, String> params) {
        try {
            bautaManager.startJob(item.getName(), params);
        }
        catch(JobParametersInvalidException e) {
            showErrorMessage(e.getMessage());
        } catch (JobInstanceAlreadyExistsException e) {
            showErrorMessage("This job is already running");
        } catch (NoSuchJobException e) {
            showErrorMessage(e.getMessage());
        }
    }
    private Component createButtons(Grid<JobInstanceInfo> grid, JobInstanceInfo item) {
        VerticalLayout vl = new VerticalLayout();
        vl.setPadding(false);
        vl.setSpacing(false);
        HorizontalLayout hl = new HorizontalLayout();
        hl.setSpacing(false);
        hl.setPadding(false);
        Button startButton = new Button("", clickEvent -> {
            if (item.hasJobParameters()) {
                Dialog d = createJobParamsDialog(item);
                d.open();
            }
            else {
                doStartJob(item, null);
            }

        });
        startButton.setIcon(VaadinIcon.PLAY.create());
        startButton.getElement().setProperty("title", "Start a new job instance");

        hl.add(startButton);

        Button stopButton = new Button("", clickEvent -> {
            try {
                bautaManager.stopJob(item.getName());
            }
            catch(Exception e) {
                showErrorMessage(e.getMessage());
            }
        });
        stopButton.setIcon(VaadinIcon.STOP.create());
        stopButton.getStyle().set("margin-left", "4px");
        stopButton.getElement().setProperty("title", "Stop a running job");

        hl.add(stopButton);

        Button restartButton = new Button("", clickEvent -> {
            try {
                bautaManager.restartJob(item.getLatestExecutionId());
            } catch (Exception e) {
                showErrorMessage(e.getMessage());
                //TODO: Error handling
                e.printStackTrace();
            }
        });
        restartButton.setIcon(VaadinIcon.ROTATE_LEFT.create());
        restartButton.getStyle().set("margin-left", "4px");
        restartButton.getElement().setProperty("title", "Restart a failed or interrupted job. Will pick up where it left off.");

        hl.add(restartButton);

        Button abandonButton = new Button("", clickEvent -> {
            try {
                bautaManager.abandonJob(item.getLatestExecutionId());
            } catch (Exception e) {
                showErrorMessage(e.getMessage());
                e.printStackTrace();
            }
        });
        abandonButton.getElement().setProperty("title", "Abandons a job. Useful when the process was killed while a job was running and is now stuck in running state.");
        abandonButton.setIcon(VaadinIcon.TRASH.create());

        abandonButton.getStyle().set("margin-left", "4px");
        hl.add(abandonButton);

        Button infoButton = new Button("", clickEvent -> {

            Dialog infoDialog = new Dialog();
            infoDialog.add(createJobHistory(item.getName()));
            infoDialog.setWidth("800px");
            infoDialog.setHeight("300px");
            infoDialog.open();
        });
        infoButton.setIcon(VaadinIcon.BULLETS.create());
        infoButton.getStyle().set("margin-left", "4px");
        hl.add(infoButton);
        if ("STARTED".equals(item.getExecutionStatus())) {
            startButton.setEnabled(false);
            restartButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
            restartButton.setEnabled(item.isRestartable());
        }
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        restartButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        stopButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        abandonButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        infoButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        vl.add(hl);

        if (item.getOptionalJobParamKeys() != null && item.getOptionalJobParamKeys().size() > 0) {
            Label l = new Label("Optional params: " + StringUtils.join(item.getOptionalJobParamKeys(),","));
            l.getStyle().set("font-size", "0.8em");
            vl.add(l);
        }
        if (item.getRequiredJobParamKeys() != null && item.getRequiredJobParamKeys().size() > 0) {
            Label l = new Label("Required params: " + StringUtils.join(item.getRequiredJobParamKeys(),","));
            l.getStyle().set("font-size", "0.8em");
            vl.add(l);
        }
        return vl;

    }

    private Component createStepComponent(Grid<JobInstanceInfo> grid, JobInstanceInfo jobInstanceInfo) {

        VerticalLayout vl = new VerticalLayout();
        vl.setAlignItems(FlexComponent.Alignment.START);
        vl.setSpacing(false);
        vl.setPadding(false);

        for (StepInfo step : jobInstanceInfo.getSteps()) {
            long diff = 0;
            if (jobInstanceInfo.getLatestExecutionId() != null && step.getJobExecutionId() != null) {
                diff = jobInstanceInfo.getLatestExecutionId() - step.getJobExecutionId();
            }

            Component statusLabel = createStatusLabel(step.getExecutionStatus(), diff > 0);

            Label stepNameLabel = new Label(step.getName());
            stepNameLabel.addClassName("step-label");

            Div div = new Div(stepNameLabel, statusLabel);
            div.addClassName("step-row");
            if (step.isFirstInSplit()) {
                div.addClassName("split-first");
            }
            else if (step.isLastInSplit()) {
                div.addClassName("split-last");
            }
            else if (step.getSplitId() != null) {
                div.addClassName("split");
            }

            div.getElement().setProperty("title", "jobInstanceId: "+step.getJobInstanceId()+", executionId: "+step.getJobExecutionId()+", duration: "+step.getDuration()+"ms");

            if (step.isRunning()) {
                //ProgressBar pb = new ProgressBar();
                //pb.setIndeterminate(true);
                //pb.setClassName("step-progress");
                //Div spinner = new Div();
                //spinner.addClassName("ring-spinner");
                //div.add(spinner);
                /*
                Icon spinner = VaadinIcon.COG.create();
                spinner.addClassName("rotate");
                spinner.getStyle()
                        .set("margin-bottom", "0")
                        .set("margin-top", "0")
                        .set("padding", "0").set("color","var(--lumo-primary-color)");
                spinner.setSize("0.8em");
                Div spinnerDiv = new Div(spinner);
                spinnerDiv.getStyle().set("margin-left", "5px").set("margin-top", "0").set("margin-bottom", "0").set("padding", "0").set("max-height","20");
                div.add(spinnerDiv);

                 */
            }
            if (step.getReportUrls() != null) {
                for (String url : step.getReportUrls()) {
                    Icon icon = null;

                    if (url.endsWith(".html")) {
                        icon = VaadinIcon.CHART.create();
                    } else if (url.endsWith(".log")) {
                        icon = VaadinIcon.FILE_PROCESS.create();
                    } else if (url.toUpperCase().endsWith(".CSV") || url.toUpperCase().endsWith(".XLSX")) {
                        icon = VaadinIcon.FILE_TABLE.create();
                    } else {
                        icon = VaadinIcon.FILE_O.create();
                    }
                    icon.setSize("1.2em");
                    Anchor reportAnchor = new Anchor("../" + url, icon);

                    reportAnchor.setTarget("reports");
                    reportAnchor.getStyle().set("font-size", "0.8em").set("margin-left", "5px");
                    div.add(reportAnchor);
                }
            }
            if (step.getExitDescription() != null && step.getExitDescription().length() > 0) {
                Icon icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
                Button descriptionButton = new Button(icon, clickEvent -> {
                    Dialog infoDialog = new Dialog();
                    infoDialog.setCloseOnEsc(true);
                    Label l = new Label(step.getExitDescription());
                    l.getStyle().set("font-size", "0.8em").set("font-family", "monospace");

                    infoDialog.add(l);
                    infoDialog.setWidth("600px");
                    infoDialog.setHeight("300px");
                    infoDialog.open();
                });
                //icon.setSize("1.2em");

                descriptionButton.setIcon(icon);
                descriptionButton.getStyle().set("font-size", "0.8em");
                descriptionButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
                div.add(descriptionButton);

            }
            vl.add(div);
        }
        return vl;
    }

    private Component createStatusLabel(String executionStatus, boolean oldExecution) {
        Div statusLabel = new Div();
        statusLabel.addClassName("step_status");
        if (oldExecution) statusLabel.addClassName("old_execution");
        statusLabel.getElement().setAttribute("data-exitstatus",executionStatus);
        statusLabel.setText(executionStatus);
        //statusLabel.getStyle().set("background-color", batchStatusToColor(executionStatus));
        return statusLabel;
    }

    private Component createJobHistory(String jobName) {
        VerticalLayout vl = new VerticalLayout();
        List<JobInstanceInfo> jobs = null;
        try {
            jobs = bautaManager.jobHistory(jobName);
        } catch (Exception e) {
            showErrorMessage("Failed to fetch job history: " + e.getMessage());
        }
        for (JobInstanceInfo ji : jobs) {
            Div div = new Div();
            div.setWidthFull();
            UnorderedList ul = new UnorderedList();


            ul.add(new ListItem("InstanceId: " + ji.getInstanceId().toString()));
            ul.add(new ListItem("ExecutionId: " + ji.getLatestExecutionId().toString()));
            ul.add(new ListItem("Start/end time: " + DateFormatUtils.format(ji.getStartTime(), "yyMMdd HH:mm:ss", Locale.US) + "/" + DateFormatUtils.format(ji.getEndTime(), "yyMMdd HH:mm:ss", Locale.US)));
            ul.add(new ListItem("Duration: " + DurationFormatUtils.formatDuration(ji.getDuration(), "HH:mm:ss")));
            ul.add(new ListItem("Params: " + ji.getJobParameters()));
            ul.add(new ListItem(new Label("Exit status: "), createStatusLabel(ji.getExitStatus(), false)));
            div.add(ul);
            Grid<StepInfo> grid = new Grid<>();
            grid.setHeightByRows(true);
            grid.setWidthFull();
            grid.addColumn(StepInfo::getName).setHeader("Name").setAutoWidth(true);
            //grid.addColumn(StepInfo::getExecutionStatus).setHeader("Status");

            grid.addComponentColumn(item -> createStatusLabel(item.getExecutionStatus(), false)).setHeader("Status");
            grid.addComponentColumn(item -> new Label(DurationFormatUtils.formatDuration(item.getDuration(), "HH:mm:ss"))).setHeader("Duration");
            grid.addComponentColumn(item -> {
                Div reportDiv = new Div();
                if (item.getReportUrls() != null) {
                    for (String url : item.getReportUrls()) {
                        Icon icon = null;

                        if (url.endsWith(".html")) {
                            icon = VaadinIcon.CHART.create();
                        } else if (url.endsWith(".log")) {
                            icon = VaadinIcon.FILE_PROCESS.create();
                        } else if (url.toUpperCase().endsWith(".CSV") || url.toUpperCase().endsWith(".XLSX")) {
                            icon = VaadinIcon.FILE_TABLE.create();
                        } else {
                            icon = VaadinIcon.FILE_O.create();
                        }
                        icon.setSize("1.2em");
                        Anchor reportAnchor = new Anchor("../" + url, icon);

                        reportAnchor.setTarget("reports");
                        reportAnchor.getStyle().set("font-size", "0.8em").set("margin-left", "5px");
                        reportDiv.add(reportAnchor);
                    }
                }
                return reportDiv;
            });
            grid.setItems(ji.getSteps());
            div.add(grid);
            vl.add(div);

        }
        return vl;
    }
    private Dialog createJobParamsDialog(JobInstanceInfo job) {
        Dialog dialog = new Dialog();
        dialog.setWidth("700");
        HashMap<String, TextField> requiredTextFields = new HashMap<>();
        HashMap<String, TextField> optionalTextFields = new HashMap<>();

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setWidthFull();

        formLayout.add(new H4("Job Parameters"));
        if (job.getRequiredJobParamKeys() != null) {
            for (String key : job.getRequiredJobParamKeys()) {
                TextField paramField = new TextField();
                paramField.setLabel(key);
                paramField.setRequired(true);
                //paramField.setRequiredIndicatorVisible(true);
                paramField.setWidthFull();
                formLayout.add(paramField);
                requiredTextFields.put(key, paramField);
            }
        }
        if (job.getOptionalJobParamKeys() != null) {
            for (String key : job.getOptionalJobParamKeys()) {
                TextField paramField = new TextField();
                paramField.setRequired(false);
                paramField.setLabel(key);
                paramField.setWidthFull();
                formLayout.add(paramField);
                optionalTextFields.put(key, paramField);
            }
        }

        Button startButton = new Button("Start", clickEvent -> {
            HashMap<String, String> params = new HashMap<>();
            for (Map.Entry<String, TextField> field:requiredTextFields.entrySet()) {
                if (StringUtils.isNotEmpty(field.getValue().getValue())) {
                    params.put(field.getKey(), field.getValue().getValue());
                }
                // TODO: Add early validation of required fields
            }
            for (Map.Entry<String, TextField> field:optionalTextFields.entrySet()) {
                if (StringUtils.isNotEmpty(field.getValue().getValue())) {
                    params.put(field.getKey(), field.getValue().getValue());
                }
            }
            dialog.close();
            doStartJob(job, params);

        });
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancel", clickEvent -> {
            dialog.close();
        });
        HorizontalLayout hl = new HorizontalLayout();
        hl.add(startButton);
        hl.add(cancelButton);
        formLayout.add(hl);
        dialog.add(formLayout);

        return dialog;
    }

    @Override
    public void onJobChange(JobInstanceInfo jobInstanceInfo) {
        log.debug("onJobChange {} ", jobInstanceInfo);

        this.getUI().get().access(() -> {
            grid.getDataProvider().refreshItem(jobInstanceInfo);
        });
    }

    private void showErrorMessage(String message) {
        Label label = new Label(message);
        Notification notification = new Notification(label);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setPosition(Notification.Position.BOTTOM_START);
        notification.setDuration(10000);
        notification.open();
    }
    private void showInfoMessage(String message) {
        Label label = new Label(message);
        Notification notification = new Notification(label);
        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        notification.setPosition(Notification.Position.BOTTOM_START);
        notification.setDuration(10000);
        notification.open();
    }

}
