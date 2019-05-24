package iss.lms.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import iss.lms.model.Employee;
import iss.lms.model.Leave;
import iss.lms.service.*;

@Controller
public class EmployeeController {

	public static ArrayList<String> publicHolidays = new ArrayList<String>() {
	{
		add("2019-05-01");
		add("2019-05-20");
		add("2019-06-05");
		add("2019-08-09");
		add("2019-08-12");
		add("2019-11-28");
		add("2019-12-25");
	}};
	@Autowired
	private EmployeeRepository er;
	@Autowired
	private LeaveRepository lr;
	
	public void setRepo(EmployeeRepository er, LeaveRepository lr)	{
		this.er = er;
		this.lr = lr;
	}

//___________________________________LOGIN FUNCTIONS___________________________________________
	
	@RequestMapping("/login")
	public String login(Model model) {
		model.addAttribute("user", new Employee());
		model.addAttribute("loginstatus","");
		return "login";
	}
	@RequestMapping("/logout")
	public String logout(Model model, @CookieValue("id") String id, HttpServletResponse hr ) {
		Cookie session = new Cookie("id",id);
		session.setMaxAge(0);
		hr.addCookie(session);
		model.addAttribute("user", new Employee());
		model.addAttribute("loginstatus","Successfully logged out.");
		return "login";
	}
	@PostMapping("/login")
	public String doLogin(@Valid @ModelAttribute ("user")Employee e, BindingResult br, Model model, HttpServletResponse hr ) {
		if(br.hasErrors()) {
			return "login";
		}
		Employee f = er.employeeLogin(e.getUsername(), e.getPassword());
		if (f == null)
		{
			model.addAttribute("loginstatus","Invalid username or password.");
			return "login";
		}
		else if(f.getEmployeeType().equals("Admin")) {
			model.addAttribute("loginstatus","Please use the admin login page instead.");
			return "login";
		}
		else {
			int id = f.getId();
			Cookie session = new Cookie("id",String.valueOf(id));
			session.setMaxAge(10000);
			hr.addCookie(session);
			return "redirect:index";
			}
	}
	
	@RequestMapping("/alogin")
	public String adminLogin(Model model) {
		model.addAttribute("user", new Employee());
		model.addAttribute("loginstatus","");
		return "alogin";
	}
	@RequestMapping("/alogout")
	public String adminLogout(Model model, @CookieValue("admin") String id, HttpServletResponse hr ) {
		Cookie session = new Cookie("admin",id);
		session.setMaxAge(0);
		hr.addCookie(session);
		model.addAttribute("user", new Employee());
		model.addAttribute("loginstatus","Successfully logged out.");
		return "alogin";
	}
	@PostMapping("/adminlogin")
	public String doAdminLogin(@Valid @ModelAttribute ("user")Employee e, BindingResult br, Model model, HttpServletResponse hr ) {
		if(br.hasErrors()) {
			return "alogin";
		}
		Employee f = er.employeeLogin(e.getUsername(), e.getPassword());
		if (f == null)
		{
			model.addAttribute("loginstatus","Invalid username or password.");
			return "alogin";
		}
		else if(!f.getEmployeeType().equals("Admin")) {
			model.addAttribute("loginstatus","You need admin permissions to log in.");
			return "alogin";
		}
		else {
			int id = f.getId();
			Cookie session = new Cookie("admin",String.valueOf(id));
			session.setMaxAge(10000);
			hr.addCookie(session);
			return "redirect:admin";
			}
	}
	
//___________________________________ADMIN FUNCTIONS___________________________________________
	
	@RequestMapping("/admin")
	public String admin(Model model, @CookieValue("admin") String id) {
		Employee userdetails = er.findEmployeeById(Integer.parseInt(id));
		List<Employee> plist = er.findAll();
		ArrayList<Employee> managers = er.findManagers();
		model.addAttribute("plist",plist);
		model.addAttribute("userdetails", userdetails);
		model.addAttribute("managers", managers);
		return "admin";
	}
		@PostMapping("/adduser")
    public String adduser(Employee e) {

		if(e.getManagerId()==0)	{
			e.setManagerId(null);
		}
		if(e.getEmployeeType().equals("Manager")) {
			e.setAnnualLeave(18);
		}
		if(e.getEmployeeType().equals("Staff"))	{
			e.setAnnualLeave(14);
		}
		e.setMedicalLeave(60);
		e.setCompLeave(0.0);
		er.save(e);		
		return "redirect:/admin";
    }
		@PostMapping("/deleteuser")
	public String deleteuser(Employee e)
	{
		if(e.getEmployeeType().equals("Manager")) {
			ArrayList<Employee> subordinates = er.findSubordinates(e.getId());
			for(Employee sub : subordinates)
			{
				sub.setManagerId(null);
			}
		}
		List<Leave> deleteuser = lr.getLeaveByEmployeeId(e.getId());
		for(Leave l : deleteuser)
		{
			lr.delete(l);
		}
		er.delete(e.getId());

	    return "redirect:/admin";
	}
		@PostMapping("/updateuser")
	public String updateuser(Employee e)
	{
		if(e.getManagerId()==0)	{
			e.setManagerId(null);
		}
		 er.modify(e.getUsername(), e.getPassword(), e.getEmployeeType(), e.getAnnualLeave(), e.getMedicalLeave(), e.getCompLeave(), e.getManagerId(), e.getId());
		 return "redirect:/admin";
	}
	
//___________________________________EMPLOYEE FUNCTIONS___________________________________________
		
	@RequestMapping("/index")
	public String index(Model model, @CookieValue("id") String id) {
		Employee e = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", e);
		return "index";
	}
	@RequestMapping("/applyform")
	public String applyForm(Model model, @CookieValue("id") String id) {
		Employee e = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", e);
		model.addAttribute("leave", new Leave());
		model.addAttribute("errormsg","");
		return "applyform";
	}
		@PostMapping("/apply")
	public String applyLeave(@Valid @ModelAttribute("leave") Leave l, BindingResult br, Model model, @CookieValue("id") String id) {
		Employee e = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", e);
		if(br.hasErrors())
		{	
			return "applyform";}
		l.setStatus("Applied");
		if(checkValidDates(l))
			if(checkNoLeaveConflict(l))
				if(processLeave(l))
					lr.save(l);
				else {
					model.addAttribute("errormsg","Insufficient leave.");
					return "applyform";}
			else {
				model.addAttribute("errormsg","Your leave conflicts with another instance of your own leave.");
				return "applyform";}
		else {
			model.addAttribute("errormsg","Your leave end date must be after your start date, and your leave must start and end on working days.");
			return "applyform";}
		return "redirect:/viewleave";
	}
	@RequestMapping("/viewleave")
	public String viewLeave(Model model, @CookieValue("id") String id) {
		Employee f = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", f);
		ArrayList<Leave> llist = lr.getLeaveByEmployeeId(Integer.parseInt(id));
		model.addAttribute("leave", llist);
		return "viewleave";
	}

		@RequestMapping("/updateform/{leaveId}")
	public String updateForm(Model model, @CookieValue("id") String id, @PathVariable(value="leaveId") int leaveid, HttpSession session) {
		Employee e = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", e);
		Leave leave = lr.getLeaveById(leaveid);
		model.addAttribute("leave", leave);
		return "updateform";
	}
		@PostMapping("/update/{leaveId}")
	public String updateLeave(@Valid Leave l, BindingResult br, Model model, @PathVariable(value="leaveId") int leaveid, @CookieValue("id") String id) {
		
		Employee e = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", e);
		model.addAttribute("leave", l);
		if(br.hasErrors())
		{	
			return "updateform";
			}
		else {
			if(checkValidDates(l)){
				
				Leave oldLeave = lr.getLeaveById(l.getLeaveid());
				oldLeave.setStatus("temp");
				lr.save(oldLeave);
				if(checkNoLeaveConflict(l))
				{	
					if(processLeave(l)) {
						refundLeave(oldLeave);
						l.setStatus("Updated");
						lr.save(l);}
					else {
						oldLeave.setStatus("Updated");
						lr.save(oldLeave);
						model.addAttribute("errormsg","Insufficient leave balance.");
						return "updateform"; }
				}
				else {
					oldLeave.setStatus("Updated");
					lr.save(oldLeave);
					model.addAttribute("errormsg","Updated leave conflicts with another instance of your own leave.");
					return "updateform";}
				}
			else {
				model.addAttribute("errormsg","Your leave end date must be after your start date, and your leave must start and end on working days.");
				return "updateform";}
		return "redirect:/viewleave";
		}
	}
	@RequestMapping("/delete/{leaveId}")
	public String deleteLeave(@CookieValue("id") String id, @PathVariable(value="leaveId") int leaveid, Model model) {
		Leave l = lr.getLeaveById(leaveid);
		l.setStatus("Deleted");
		lr.save(l);
		refundLeave(l);
		return "redirect:/viewleave";
	}
	@RequestMapping("/cancel/{leaveId}")
	public String cancelLeave(@CookieValue("id") String id, @PathVariable(value="leaveId") int leaveid, Model model) {
		Leave l = lr.getLeaveById(leaveid);
		l.setStatus("Cancelled");
		lr.save(l);
		refundLeave(l);
		return "redirect:/viewleave";
	}
	
//___________________________________MANAGER FUNCTIONS___________________________________________	
	
	@RequestMapping("/viewsubleave")
	public String viewSubLeave(Model model, @CookieValue("id") String id) {
		Employee f = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", f);
		ArrayList<Employee> elist = er.findSubordinates(Integer.parseInt(id));
		ArrayList<Leave> llist = new ArrayList<Leave>();
		for(Employee sub : elist) {
			ArrayList<Leave> leaveperemployee = lr.getLeaveByEmployeeId(sub.getId());
			llist.addAll(leaveperemployee);
		}
		model.addAttribute("subleave", llist);
		return "viewsubleave";
	}
	@RequestMapping("/viewpending")
	public String viewPendingApprovals(Model model, @CookieValue("id") String id) {
		Employee f = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", f);
		ArrayList<Employee> elist = er.findSubordinates(Integer.parseInt(id));
		ArrayList<Leave> llist = new ArrayList<Leave>();
		for(Employee sub : elist) {
			ArrayList<Leave> leaveperemployee = lr.getLeaveByEmployeeId(sub.getId());
			llist.addAll(leaveperemployee);
		}
		model.addAttribute("subleave", llist);
		return "viewpending";
	}
		@RequestMapping("/approveform/{leaveId}")
	public String approveForm(Model model, @CookieValue("id") String id, @PathVariable(value="leaveId") int leaveid) {
		Employee e = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", e);
		Leave l = lr.getLeaveById(leaveid);
		model.addAttribute("leave", l);
		String subname = er.findEmployeeById(l.getEmployeeid()).getUsername();
		model.addAttribute("subordinate", subname);
		
		
		ArrayList<Employee> subordinates = er.findSubordinates(Integer.parseInt(id));
		ArrayList<Leave> allsubordinateleaves = new ArrayList<Leave>();
		for(Employee sub : subordinates) {
			ArrayList<Leave> leaveperemployee = lr.getLeaveByEmployeeId(sub.getId());
			allsubordinateleaves.addAll(leaveperemployee);
		}
		ArrayList<Leave> conflictingleaves = new ArrayList<Leave>();
		for(Leave others : allsubordinateleaves) {
			if(checkOthersOnLeave(l, others))
				conflictingleaves.add(others);
		}
		model.addAttribute("conflicts", conflictingleaves);	
		
		return "approveform";
	}
	@RequestMapping("/rejectform/{leaveId}")
	public String rejectForm(Model model, @CookieValue("id") String id, @PathVariable(value="leaveId") int leaveid) {
		Employee e = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", e);
		Leave l = lr.getLeaveById(leaveid);
		model.addAttribute("leave", l);
		String subname = er.findEmployeeById(l.getEmployeeid()).getUsername();
		model.addAttribute("subordinate", subname);
		
		
		ArrayList<Employee> subordinates = er.findSubordinates(Integer.parseInt(id));
		ArrayList<Leave> allsubordinateleaves = new ArrayList<Leave>();
		for(Employee sub : subordinates) {
			ArrayList<Leave> leaveperemployee = lr.getLeaveByEmployeeId(sub.getId());
			allsubordinateleaves.addAll(leaveperemployee);
		}
		ArrayList<Leave> conflictingleaves = new ArrayList<Leave>();
		for(Leave others : allsubordinateleaves) {
			if(checkOthersOnLeave(l, others))
				conflictingleaves.add(others);
		}
		model.addAttribute("conflicts", conflictingleaves);	
		
		
		
		return "rejectform";
	}
	@PostMapping("/approve/{leaveId}")
	public String approveLeave(@ModelAttribute("leave") Leave l, Model model, @CookieValue("id") String id, @PathVariable(value="leaveId") int leaveid) {
		l.setStatus("Approved");
		lr.save(l);
		return "redirect:/viewsubleave";
	}
	@PostMapping("/reject/{leaveId}")
	public String rejectLeave(@ModelAttribute("leave") Leave l, Model model, @CookieValue("id") String id, @PathVariable(value="leaveId") int leaveid) {
		Employee e = er.findEmployeeById(Integer.parseInt(id));
		model.addAttribute("user", e);
		model.addAttribute("leave", l);
		String subname = er.findEmployeeById(l.getEmployeeid()).getUsername();
		model.addAttribute("subordinate", subname);
		
		
		ArrayList<Employee> subordinates = er.findSubordinates(Integer.parseInt(id));
		ArrayList<Leave> allsubordinateleaves = new ArrayList<Leave>();
		for(Employee sub : subordinates) {
			ArrayList<Leave> leaveperemployee = lr.getLeaveByEmployeeId(sub.getId());
			allsubordinateleaves.addAll(leaveperemployee);
		}
		ArrayList<Leave> conflictingleaves = new ArrayList<Leave>();
		for(Leave others : allsubordinateleaves) {
			if(checkOthersOnLeave(l, others))
				conflictingleaves.add(others);
		}
		model.addAttribute("conflicts", conflictingleaves);	
				
		
		if(l.getManagerComment().equals(""))
		{
			model.addAttribute("errormsg","You need to enter comments if rejecting a leave application.");
			return "rejectform";}
		else {
		l.setStatus("Rejected");
		lr.save(l);
		refundLeave(l);}
		return "redirect:/viewsubleave";
	}
	
//___________________________________UTILITY FUNCTIONS___________________________________________		
	
	public int computeDays(Leave l)
	{
		LocalDate i = l.getStartDate();
		LocalDate j = l.getEndDate();
		long daysLeave = ChronoUnit.DAYS.between(i, j.plusDays(1));
			if(daysLeave <=14) {
				for(LocalDate k = i; !k.isAfter(j); k = k.plusDays(1))
				{
					if(k.getDayOfWeek().equals(DayOfWeek.SATURDAY) || k.getDayOfWeek().equals(DayOfWeek.SUNDAY) || publicHolidays.contains(k.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
						daysLeave = daysLeave - 1;
				}
			}
	return (int) daysLeave;
	}
	public boolean processLeave(Leave l)
	{
		Employee e = er.findEmployeeById(l.getEmployeeid());
		int daysLeave = computeDays(l);
		if(l.getLeaveType().equals("Annual")) {
			if(e.getAnnualLeave() >= daysLeave) {
				e.setAnnualLeave(e.getAnnualLeave() - daysLeave);
				er.save(e);
				return true;
				}
			else return false;
		}
		else if(l.getLeaveType().equals("Medical")) {
			if(e.getMedicalLeave()>= daysLeave) {
				e.setMedicalLeave(e.getMedicalLeave() - daysLeave);
				er.save(e);
				return true;
				}
			else return false;
		}
		else if(l.getLeaveType().equals("Compensation")){
			if(e.getCompLeave() >= daysLeave) {
				e.setCompLeave(e.getCompLeave() - daysLeave);
				er.save(e);
				return true;
				}
			else return false;
		}
		else return false;
	}
	public void refundLeave(Leave l) {
		Employee e = er.findEmployeeById(l.getEmployeeid());
		int daysLeave = computeDays(l);
		if(l.getLeaveType().equals("Annual")) {
			e.setAnnualLeave(e.getAnnualLeave() + daysLeave);
		}
		else if(l.getLeaveType().equals("Medical")) {
			e.setMedicalLeave(e.getMedicalLeave() + daysLeave);
		}
		else if(l.getLeaveType().equals("Compensation")) {
			e.setCompLeave(e.getCompLeave() + daysLeave);
		}
		er.save(e);
	}
	public boolean checkNoLeaveConflict(Leave l) {
		ArrayList<Leave> allleave = lr.getLeaveByEmployeeId(l.getEmployeeid());
		for(Leave m : allleave)
		{
			if(!m.getStartDate().isAfter(l.getEndDate()) && !m.getEndDate().isBefore(l.getStartDate()) && (m.getStatus().equals("Applied") || m.getStatus().equals("Updated") || m.getStatus().equals("Approved")))
				return false;
		}
		return true;
	}
	public boolean checkOthersOnLeave(Leave toreview, Leave other) {
		if(!toreview.getStartDate().isAfter(other.getEndDate()) && !toreview.getEndDate().isBefore(other.getStartDate()) && other.getStatus().equals("Approved"))
			return true;
		return false;
	}
	public boolean checkValidDates(Leave l)
	{
		LocalDate i = l.getStartDate();
		LocalDate j = l.getEndDate();
		if(i.isAfter(j))
			return false;
		if(i.getDayOfWeek().equals(DayOfWeek.SATURDAY) || i.getDayOfWeek().equals(DayOfWeek.SUNDAY) ||j.getDayOfWeek().equals(DayOfWeek.SATURDAY) || j.getDayOfWeek().equals(DayOfWeek.SUNDAY) || publicHolidays.contains(i.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) || publicHolidays.contains(j.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
			return false;
		return true;
	}
}