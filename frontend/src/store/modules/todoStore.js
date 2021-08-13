import Todo from "@/api/Todo";

export default {
  namespaced: true,
  state: {
    // data 가 들어가는 곳
    todoLists: [],
    todayCommit: null,
  },
  mutations: {
    // 여기서 data 를 업데이트
    SET_TODO(state, value) {
      state.todoLists = value;
    },
    CREATE_TODO(state, newTodo) {
      state.todoLists.push(newTodo);
    },
    UPDATE_TODO(state, editTodo) {
      const index = state.todoLists.findIndex((todo) => todo.todoid === editTodo.todoid);

      if (index !== -1) {
        state.todoLists.splice(index, 1, editTodo);
      }
    },
    DELETE_TODO(state, todoid) {
      state.todoLists = state.todoLists.filter((todo) => todo.todoid !== todoid);
    },
    SET_TODAY_COMMIT(state, flag) {
      state.todayCommit = flag;
    },
  },
  actions: {
    // 메소드가 들어가는 곳
    setTodoList({ commit }, data) {
      Todo.loadTodoList(
        data,
        (res) => {
          commit("SET_TODO", res.data.list);
        },
        (err) => {
          alert(err);
        }
      );
    },
    createTodo({ commit }, payload) {
      Todo.createTodo(
        payload,
        (res) => {
          if (res.data.status === "fail") alert("create fail");
          commit("CREATE_TODO", res.data.todo);
        },
        (err) => {
          alert(err);
        }
      );
    },
    updateTodo({ commit }, payload) {
      Todo.updateTodo(
        payload,
        (res) => {
          if (res.data.status === "fail") alert("update fail");
          commit("UPDATE_TODO", res.data.todo);
        },
        (err) => {
          alert(err);
        }
      );
    },
    deleteTodo({ commit }, payload) {
      Todo.deleteTodo(
        payload,
        (res) => {
          if (res.data.status === "fail") alert("delete fail");
          commit("DELETE_TODO", payload);
        },
        (err) => {
          alert(err);
        }
      );
    },
    refreshCommit({ commit }) {
      Todo.getCommitHistory(
        (res) => {
          const day1 = res.data.commit.day1;
          commit("SET_TODAY_COMMIT", day1 > 0 ? true : false);
        },
        (err) => {
          alert(err);
        }
      );
    },
  },
  getters: {},
};
